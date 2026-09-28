package com.mentorconnect.service;

import com.mentorconnect.dto.AlumniRequestDto;
import com.mentorconnect.dto.AlumniResponseDto;
import com.mentorconnect.dto.InterestTagDto;
import com.mentorconnect.exception.DuplicateResourceException;
import com.mentorconnect.exception.InvalidOperationException;
import com.mentorconnect.exception.ResourceNotFoundException;
import com.mentorconnect.model.Alumni;
import com.mentorconnect.model.InterestTag;
import com.mentorconnect.model.PairStatus;
import com.mentorconnect.repository.AlumniRepository;
import com.mentorconnect.repository.MentorshipPairRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlumniService {

    private final AlumniRepository alumniRepository;
    private final MentorshipPairRepository mentorshipPairRepository;
    private final InterestTagService interestTagService;
    private final AuditLogService auditLogService;

    public AlumniService(AlumniRepository alumniRepository,
                         MentorshipPairRepository mentorshipPairRepository,
                         InterestTagService interestTagService,
                         AuditLogService auditLogService) {
        this.alumniRepository = alumniRepository;
        this.mentorshipPairRepository = mentorshipPairRepository;
        this.interestTagService = interestTagService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AlumniResponseDto> getAllAlumni(String search, String tag) {
        List<Alumni> all = alumniRepository.findAllWithTags();
        return all.stream()
                .filter(a -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String s = search.trim().toLowerCase();
                    return a.getName().toLowerCase().contains(s) ||
                            a.getCompany().toLowerCase().contains(s) ||
                            a.getJobTitle().toLowerCase().contains(s) ||
                            a.getEmail().toLowerCase().contains(s);
                })
                .filter(a -> {
                    if (tag == null || tag.trim().isEmpty()) return true;
                    String t = tag.trim().toLowerCase();
                    return a.getExpertiseTags().stream()
                            .anyMatch(tg -> tg.getName().toLowerCase().contains(t));
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlumniResponseDto getAlumniById(Long id) {
        Alumni alumni = alumniRepository.findByIdWithTags(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumni not found with id: " + id));
        return toDto(alumni);
    }

    @Transactional(readOnly = true)
    public Page<AlumniResponseDto> getAlumniPaged(Pageable pageable) {
        return alumniRepository.findAll(pageable).map(this::toDto);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public AlumniResponseDto createAlumni(AlumniRequestDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (alumniRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Alumni already exists with email: " + email);
        }

        Alumni alumni = new Alumni();
        alumni.setName(dto.getName().trim());
        alumni.setEmail(email);
        alumni.setGraduationYear(dto.getGraduationYear());
        alumni.setCompany(dto.getCompany().trim());
        alumni.setJobTitle(dto.getJobTitle().trim());
        alumni.setMaxConcurrentMentees(dto.getMaxConcurrentMentees());
        alumni.setBio(dto.getBio());
        alumni.setLinkedinUrl(dto.getLinkedinUrl());

        if (dto.getExpertiseTags() != null) {
            Set<InterestTag> tags = new HashSet<>();
            for (String tagName : dto.getExpertiseTags()) {
                if (tagName != null && !tagName.trim().isEmpty()) {
                    tags.add(interestTagService.getOrCreateTag(tagName.trim()));
                }
            }
            alumni.setExpertiseTags(tags);
        }

        alumni = alumniRepository.save(alumni);
        auditLogService.recordLog(
                "ALUMNI_REGISTERED",
                "ALUMNI",
                alumni.getId(),
                "Registered alumni: " + alumni.getName() + " (" + alumni.getCompany() + ", " + alumni.getJobTitle() + ")"
        );
        return toDto(alumni);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public AlumniResponseDto updateAlumni(Long id, AlumniRequestDto dto) {
        Alumni alumni = alumniRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumni not found with id: " + id));

        String email = dto.getEmail().trim().toLowerCase();
        if (!alumni.getEmail().equalsIgnoreCase(email) && alumniRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Another alumni already registered with email: " + email);
        }

        long activeCount = mentorshipPairRepository.countByAlumniIdAndStatus(id, PairStatus.ACTIVE);
        if (dto.getMaxConcurrentMentees() < activeCount) {
            throw new InvalidOperationException("Cannot reduce maximum concurrent mentees below current active mentees count (" + activeCount + ")");
        }

        alumni.setName(dto.getName().trim());
        alumni.setEmail(email);
        alumni.setGraduationYear(dto.getGraduationYear());
        alumni.setCompany(dto.getCompany().trim());
        alumni.setJobTitle(dto.getJobTitle().trim());
        alumni.setMaxConcurrentMentees(dto.getMaxConcurrentMentees());
        alumni.setBio(dto.getBio());
        alumni.setLinkedinUrl(dto.getLinkedinUrl());

        if (dto.getExpertiseTags() != null) {
            Set<InterestTag> tags = new HashSet<>();
            for (String tagName : dto.getExpertiseTags()) {
                if (tagName != null && !tagName.trim().isEmpty()) {
                    tags.add(interestTagService.getOrCreateTag(tagName.trim()));
                }
            }
            alumni.setExpertiseTags(tags);
        }

        alumni = alumniRepository.save(alumni);
        return toDto(alumni);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public void deleteAlumni(Long id) {
        Alumni alumni = alumniRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumni not found with id: " + id));

        long totalPairs = mentorshipPairRepository.countByAlumniId(id);
        if (totalPairs > 0) {
            throw new InvalidOperationException("Cannot delete alumni '" + alumni.getName() + "' because they have " + totalPairs + " mentorship pair record(s). Mentorship history must be preserved.");
        }

        alumni.getExpertiseTags().clear();
        alumniRepository.save(alumni);
        alumniRepository.delete(alumni);
    }

    public AlumniResponseDto toDto(Alumni alumni) {
        AlumniResponseDto dto = new AlumniResponseDto();
        dto.setId(alumni.getId());
        dto.setName(alumni.getName());
        dto.setEmail(alumni.getEmail());
        dto.setGraduationYear(alumni.getGraduationYear());
        dto.setCompany(alumni.getCompany());
        dto.setJobTitle(alumni.getJobTitle());
        dto.setMaxConcurrentMentees(alumni.getMaxConcurrentMentees());
        dto.setBio(alumni.getBio());
        dto.setLinkedinUrl(alumni.getLinkedinUrl());

        if (alumni.getExpertiseTags() != null) {
            dto.setExpertiseTags(alumni.getExpertiseTags().stream()
                    .map(interestTagService::toDto)
                    .collect(Collectors.toSet()));
        }

        long activeCount = mentorshipPairRepository.countByAlumniIdAndStatus(alumni.getId(), PairStatus.ACTIVE);
        dto.setActiveMenteeCount((int) activeCount);
        int available = Math.max(0, alumni.getMaxConcurrentMentees() - (int) activeCount);
        dto.setAvailableSlots(available);

        return dto;
    }
}
