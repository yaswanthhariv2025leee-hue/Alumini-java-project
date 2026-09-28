package com.mentorconnect.service;

import com.mentorconnect.dto.MatchSuggestionDto;
import com.mentorconnect.dto.MentorshipPairRequestDto;
import com.mentorconnect.dto.MentorshipPairResponseDto;
import com.mentorconnect.exception.DuplicateResourceException;
import com.mentorconnect.exception.MentorshipCapacityExceededException;
import com.mentorconnect.exception.ResourceNotFoundException;
import com.mentorconnect.model.*;
import com.mentorconnect.repository.AlumniRepository;
import com.mentorconnect.repository.MentorshipPairRepository;
import com.mentorconnect.repository.SessionRepository;
import com.mentorconnect.repository.StudentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchingService {

    private final AlumniRepository alumniRepository;
    private final StudentRepository studentRepository;
    private final MentorshipPairRepository mentorshipPairRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogService auditLogService;

    public MatchingService(AlumniRepository alumniRepository,
                           StudentRepository studentRepository,
                           MentorshipPairRepository mentorshipPairRepository,
                           SessionRepository sessionRepository,
                           AuditLogService auditLogService) {
        this.alumniRepository = alumniRepository;
        this.studentRepository = studentRepository;
        this.mentorshipPairRepository = mentorshipPairRepository;
        this.sessionRepository = sessionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<MatchSuggestionDto> suggestMentorsForStudent(Long studentId) {
        Student student = studentRepository.findByIdWithTags(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Set<String> studentTagNames = student.getInterestTags().stream()
                .map(t -> t.getName().trim().toLowerCase())
                .collect(Collectors.toSet());

        return rankMentorsForTags(studentTagNames, studentId);
    }

    @Transactional(readOnly = true)
    public List<MatchSuggestionDto> suggestMentorsByTags(List<String> tags) {
        if (tags == null) {
            tags = Collections.emptyList();
        }
        Set<String> searchTags = tags.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .map(t -> t.trim().toLowerCase())
                .collect(Collectors.toSet());

        return rankMentorsForTags(searchTags, null);
    }

    private List<MatchSuggestionDto> rankMentorsForTags(Set<String> searchTags, Long studentId) {
        List<Alumni> alumniList = alumniRepository.findAllWithTags();
        List<MatchSuggestionDto> suggestions = new ArrayList<>();

        for (Alumni alumni : alumniList) {
            MatchSuggestionDto dto = new MatchSuggestionDto();
            dto.setAlumniId(alumni.getId());
            dto.setAlumniName(alumni.getName());
            dto.setEmail(alumni.getEmail());
            dto.setCompany(alumni.getCompany());
            dto.setJobTitle(alumni.getJobTitle());
            dto.setGraduationYear(alumni.getGraduationYear());
            dto.setMaxConcurrentMentees(alumni.getMaxConcurrentMentees());

            long activeCount = mentorshipPairRepository.countByAlumniIdAndStatus(alumni.getId(), PairStatus.ACTIVE);
            dto.setActiveMentees((int) activeCount);
            int available = Math.max(0, alumni.getMaxConcurrentMentees() - (int) activeCount);
            dto.setAvailableSlots(available);

            boolean alreadyMatched = studentId != null &&
                    mentorshipPairRepository.existsByAlumniIdAndStudentIdAndStatus(alumni.getId(), studentId, PairStatus.ACTIVE);
            dto.setAlreadyMatched(alreadyMatched);
            dto.setEligible(available > 0 && !alreadyMatched);

            List<String> allTags = alumni.getExpertiseTags().stream()
                    .map(InterestTag::getName)
                    .collect(Collectors.toList());
            dto.setAllExpertiseTags(allTags);

            List<String> matching = new ArrayList<>();
            for (InterestTag tag : alumni.getExpertiseTags()) {
                if (searchTags.contains(tag.getName().trim().toLowerCase())) {
                    matching.add(tag.getName());
                }
            }
            dto.setMatchingTags(matching);
            dto.setMatchCount(matching.size());

            if (!searchTags.isEmpty()) {
                double pct = ((double) matching.size() / searchTags.size()) * 100.0;
                dto.setMatchPercentage(BigDecimal.valueOf(pct).setScale(1, RoundingMode.HALF_UP).doubleValue());
            } else {
                dto.setMatchPercentage(0.0);
            }

            suggestions.add(dto);
        }

        // Rank suggestions:
        // 1. Tag overlap count (highest first - business rule)
        // 2. Eligible status (available slots & not already matched first)
        // 3. Available slots (highest first)
        // 4. Alumni name alphabetical
        suggestions.sort(
                Comparator.comparingInt(MatchSuggestionDto::getMatchCount).reversed()
                        .thenComparing(Comparator.comparing(MatchSuggestionDto::isEligible).reversed())
                        .thenComparing(Comparator.comparingInt(MatchSuggestionDto::getAvailableSlots).reversed())
                        .thenComparing(MatchSuggestionDto::getAlumniName, String.CASE_INSENSITIVE_ORDER)
        );

        return suggestions;
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public MentorshipPairResponseDto createPair(MentorshipPairRequestDto request) {
        Long alumniId = request.getAlumniId();
        Long studentId = request.getStudentId();

        // Lock alumni row with PESSIMISTIC_WRITE to avoid concurrent slot overallocation
        Alumni alumni = alumniRepository.findByIdForUpdate(alumniId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumni not found with id: " + alumniId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        // Check if an active mentorship pair already exists
        if (mentorshipPairRepository.existsByAlumniIdAndStudentIdAndStatus(alumniId, studentId, PairStatus.ACTIVE)) {
            throw new DuplicateResourceException(
                    "An active mentorship pair already exists between Alumni '" + alumni.getName() +
                            "' and Student '" + student.getName() + "'."
            );
        }

        // Check mentor concurrent mentee capacity limit
        long activeMentees = mentorshipPairRepository.countByAlumniIdAndStatus(alumniId, PairStatus.ACTIVE);
        if (activeMentees >= alumni.getMaxConcurrentMentees()) {
            throw new MentorshipCapacityExceededException(
                    "Cannot match student. Mentor '" + alumni.getName() +
                            "' has reached their maximum concurrent mentee limit of " + alumni.getMaxConcurrentMentees() +
                            " (Active mentees: " + activeMentees + ")."
            );
        }

        MentorshipPair pair = new MentorshipPair(alumni, student, request.getNotes());
        pair = mentorshipPairRepository.save(pair);

        auditLogService.recordLog(
                "PAIR_CREATED",
                "MENTORSHIP_PAIR",
                pair.getId(),
                "Created mentorship pair between Mentor '" + alumni.getName() + "' and Student '" + student.getName() + "'"
        );

        return toPairDto(pair);
    }

    public MentorshipPairResponseDto toPairDto(MentorshipPair pair) {
        MentorshipPairResponseDto dto = new MentorshipPairResponseDto();
        dto.setId(pair.getId());
        dto.setAlumniId(pair.getAlumni().getId());
        dto.setAlumniName(pair.getAlumni().getName());
        dto.setAlumniEmail(pair.getAlumni().getEmail());
        dto.setAlumniCompany(pair.getAlumni().getCompany());
        dto.setAlumniJobTitle(pair.getAlumni().getJobTitle());

        dto.setStudentId(pair.getStudent().getId());
        dto.setStudentName(pair.getStudent().getName());
        dto.setStudentEmail(pair.getStudent().getEmail());
        dto.setStudentDepartment(pair.getStudent().getDepartment());

        dto.setStatus(pair.getStatus());
        dto.setStartDate(pair.getStartDate());
        dto.setEndDate(pair.getEndDate());
        dto.setNotes(pair.getNotes());

        long total = sessionRepository.countByMentorshipPairId(pair.getId());
        long completed = sessionRepository.countByMentorshipPairIdAndStatus(pair.getId(), SessionStatus.COMPLETED);
        dto.setTotalSessions((int) total);
        dto.setCompletedSessions((int) completed);

        return dto;
    }
}
