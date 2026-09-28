package com.mentorconnect.service;

import com.mentorconnect.dto.SessionRequestDto;
import com.mentorconnect.dto.SessionResponseDto;
import com.mentorconnect.exception.InvalidOperationException;
import com.mentorconnect.exception.ResourceNotFoundException;
import com.mentorconnect.model.MentorshipPair;
import com.mentorconnect.model.PairStatus;
import com.mentorconnect.model.Session;
import com.mentorconnect.model.SessionStatus;
import com.mentorconnect.repository.MentorshipPairRepository;
import com.mentorconnect.repository.SessionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MentorshipPairRepository mentorshipPairRepository;
    private final AuditLogService auditLogService;

    public SessionService(SessionRepository sessionRepository,
                          MentorshipPairRepository mentorshipPairRepository,
                          AuditLogService auditLogService) {
        this.sessionRepository = sessionRepository;
        this.mentorshipPairRepository = mentorshipPairRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<SessionResponseDto> getAllSessions(Long pairId, SessionStatus status) {
        List<Session> list;
        if (pairId != null) {
            list = sessionRepository.findByMentorshipPairId(pairId);
            if (status != null) {
                list = list.stream().filter(s -> s.getStatus() == status).collect(Collectors.toList());
            }
        } else if (status != null) {
            list = sessionRepository.findByStatus(status);
        } else {
            list = sessionRepository.findAll();
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SessionResponseDto> getSessionsPaged(Pageable pageable, Long pairId, SessionStatus status) {
        Page<Session> paged;
        if (pairId != null && status != null) {
            paged = sessionRepository.findByMentorshipPairIdAndStatus(pairId, status, pageable);
        } else if (pairId != null) {
            paged = sessionRepository.findByMentorshipPairId(pairId, pageable);
        } else if (status != null) {
            paged = sessionRepository.findByStatus(status, pageable);
        } else {
            paged = sessionRepository.findAll(pageable);
        }
        return paged.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public SessionResponseDto getSessionById(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
        return toDto(session);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public SessionResponseDto scheduleSession(SessionRequestDto request) {
        Long pairId = request.getMentorshipPairId();
        MentorshipPair pair = mentorshipPairRepository.findById(pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship pair not found with id: " + pairId));

        if (pair.getStatus() != PairStatus.ACTIVE) {
            throw new InvalidOperationException("Cannot schedule a session for an inactive mentorship pair (Current status: " + pair.getStatus() + "). Only ACTIVE pairs can hold sessions.");
        }

        Session session = new Session();
        session.setMentorshipPair(pair);
        session.setSessionDate(request.getSessionDate());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setTopic(request.getTopic().trim());
        session.setMeetingLink(request.getMeetingLink());
        session.setFeedbackNotes(request.getFeedbackNotes());
        session.setStatus(SessionStatus.SCHEDULED);

        session = sessionRepository.save(session);

        auditLogService.recordLog(
                "SESSION_SCHEDULED",
                "SESSION",
                session.getId(),
                "Scheduled session on topic: '" + session.getTopic() + "' for Pair #" + pair.getId() +
                        " (" + pair.getAlumni().getName() + " & " + pair.getStudent().getName() + ")"
        );

        return toDto(session);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public SessionResponseDto updateSessionStatus(Long id, SessionStatus newStatus, String feedbackNotes) {
        if (newStatus == null) {
            throw new InvalidOperationException("New session status cannot be null");
        }

        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));

        session.setStatus(newStatus);
        if (feedbackNotes != null && !feedbackNotes.trim().isEmpty()) {
            session.setFeedbackNotes(feedbackNotes.trim());
        }

        session = sessionRepository.save(session);

        String action = newStatus == SessionStatus.COMPLETED ? "SESSION_COMPLETED"
                : (newStatus == SessionStatus.CANCELLED ? "SESSION_CANCELLED" : "SESSION_STATUS_CHANGED");
        auditLogService.recordLog(
                action,
                "SESSION",
                session.getId(),
                "Session #" + session.getId() + " status updated to " + newStatus
        );

        return toDto(session);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public SessionResponseDto updateSession(Long id, SessionRequestDto request) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));

        session.setSessionDate(request.getSessionDate());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setTopic(request.getTopic().trim());
        session.setMeetingLink(request.getMeetingLink());
        if (request.getFeedbackNotes() != null) {
            session.setFeedbackNotes(request.getFeedbackNotes());
        }

        session = sessionRepository.save(session);
        return toDto(session);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public void deleteSession(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
        sessionRepository.delete(session);
    }

    public SessionResponseDto toDto(Session session) {
        SessionResponseDto dto = new SessionResponseDto();
        dto.setId(session.getId());
        dto.setMentorshipPairId(session.getMentorshipPair().getId());
        dto.setAlumniId(session.getMentorshipPair().getAlumni().getId());
        dto.setAlumniName(session.getMentorshipPair().getAlumni().getName());
        dto.setStudentId(session.getMentorshipPair().getStudent().getId());
        dto.setStudentName(session.getMentorshipPair().getStudent().getName());
        dto.setSessionDate(session.getSessionDate());
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setTopic(session.getTopic());
        dto.setStatus(session.getStatus());
        dto.setMeetingLink(session.getMeetingLink());
        dto.setFeedbackNotes(session.getFeedbackNotes());
        return dto;
    }
}
