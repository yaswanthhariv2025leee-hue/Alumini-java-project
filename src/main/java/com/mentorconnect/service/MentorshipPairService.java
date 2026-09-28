package com.mentorconnect.service;

import com.mentorconnect.dto.MentorshipPairResponseDto;
import com.mentorconnect.exception.DuplicateResourceException;
import com.mentorconnect.exception.InvalidOperationException;
import com.mentorconnect.exception.MentorshipCapacityExceededException;
import com.mentorconnect.exception.ResourceNotFoundException;
import com.mentorconnect.model.Alumni;
import com.mentorconnect.model.MentorshipPair;
import com.mentorconnect.model.PairStatus;
import com.mentorconnect.model.Session;
import com.mentorconnect.model.SessionStatus;
import com.mentorconnect.repository.AlumniRepository;
import com.mentorconnect.repository.MentorshipPairRepository;
import com.mentorconnect.repository.SessionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MentorshipPairService {

    private final MentorshipPairRepository mentorshipPairRepository;
    private final MatchingService matchingService;
    private final AlumniRepository alumniRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogService auditLogService;

    public MentorshipPairService(MentorshipPairRepository mentorshipPairRepository,
                                 MatchingService matchingService,
                                 AlumniRepository alumniRepository,
                                 SessionRepository sessionRepository,
                                 AuditLogService auditLogService) {
        this.mentorshipPairRepository = mentorshipPairRepository;
        this.matchingService = matchingService;
        this.alumniRepository = alumniRepository;
        this.sessionRepository = sessionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<MentorshipPairResponseDto> getAllPairs(PairStatus status, Long alumniId, Long studentId) {
        List<MentorshipPair> pairs = mentorshipPairRepository.findAll();
        return pairs.stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> alumniId == null || p.getAlumni().getId().equals(alumniId))
                .filter(p -> studentId == null || p.getStudent().getId().equals(studentId))
                .map(matchingService::toPairDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MentorshipPairResponseDto getPairById(Long id) {
        MentorshipPair pair = mentorshipPairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship pair not found with id: " + id));
        return matchingService.toPairDto(pair);
    }

    @Transactional(readOnly = true)
    public Page<MentorshipPairResponseDto> getPairsPaged(Pageable pageable) {
        return mentorshipPairRepository.findAll(pageable).map(matchingService::toPairDto);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public MentorshipPairResponseDto updatePairStatus(Long id, PairStatus newStatus) {
        MentorshipPair pair = mentorshipPairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship pair not found with id: " + id));

        if (newStatus == null) {
            throw new InvalidOperationException("New pair status cannot be null");
        }

        if (pair.getStatus() != PairStatus.ACTIVE && newStatus == PairStatus.ACTIVE) {
            // Check if active pair between this alumni and student already exists
            boolean alreadyActive = mentorshipPairRepository.existsByAlumniIdAndStudentIdAndStatus(
                    pair.getAlumni().getId(), pair.getStudent().getId(), PairStatus.ACTIVE);
            if (alreadyActive) {
                throw new DuplicateResourceException(
                        "An active mentorship pair already exists between Alumni '" + pair.getAlumni().getName() +
                                "' and Student '" + pair.getStudent().getName() + "'."
                );
            }

            // Re-activating pair: must re-check mentor capacity with pessimistic lock
            Long alumniId = pair.getAlumni().getId();
            Alumni alumni = alumniRepository.findByIdForUpdate(alumniId)
                    .orElseThrow(() -> new ResourceNotFoundException("Alumni not found with id: " + alumniId));
            long activeMentees = mentorshipPairRepository.countByAlumniIdAndStatus(alumni.getId(), PairStatus.ACTIVE);
            if (activeMentees >= alumni.getMaxConcurrentMentees()) {
                throw new MentorshipCapacityExceededException(
                        "Cannot re-activate pair. Mentor '" + alumni.getName() +
                                "' is already at maximum concurrent mentee limit (" + alumni.getMaxConcurrentMentees() + ")."
                );
            }
            pair.setEndDate(null);
        } else if (newStatus == PairStatus.CANCELLED) {
            pair.setEndDate(LocalDate.now());
            // Automatically cancel pending scheduled sessions under this cancelled pair
            List<Session> scheduledSessions = sessionRepository.findByMentorshipPairId(pair.getId()).stream()
                    .filter(s -> s.getStatus() == SessionStatus.SCHEDULED)
                    .collect(Collectors.toList());
            for (Session s : scheduledSessions) {
                s.setStatus(SessionStatus.CANCELLED);
                if (s.getFeedbackNotes() == null || s.getFeedbackNotes().trim().isEmpty()) {
                    s.setFeedbackNotes("Automatically cancelled due to mentorship pair cancellation.");
                }
                sessionRepository.save(s);
                auditLogService.recordLog(
                        "SESSION_CANCELLED",
                        "SESSION",
                        s.getId(),
                        "Session #" + s.getId() + " automatically cancelled due to Pair #" + pair.getId() + " cancellation"
                );
            }
        } else if (newStatus == PairStatus.COMPLETED) {
            pair.setEndDate(LocalDate.now());
        }

        pair.setStatus(newStatus);
        pair = mentorshipPairRepository.save(pair);

        auditLogService.recordLog(
                "PAIR_STATUS_CHANGED",
                "MENTORSHIP_PAIR",
                pair.getId(),
                "Updated mentorship pair #" + pair.getId() + " status to " + newStatus
        );

        return matchingService.toPairDto(pair);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public void deletePair(Long id) {
        MentorshipPair pair = mentorshipPairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship pair not found with id: " + id));

        // Cleanly delete all sessions under this pair first
        List<Session> sessions = sessionRepository.findByMentorshipPairId(id);
        if (!sessions.isEmpty()) {
            sessionRepository.deleteAll(sessions);
        }
        mentorshipPairRepository.delete(pair);
    }
}
