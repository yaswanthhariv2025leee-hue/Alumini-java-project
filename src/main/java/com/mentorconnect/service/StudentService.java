package com.mentorconnect.service;

import com.mentorconnect.dto.StudentRequestDto;
import com.mentorconnect.dto.StudentResponseDto;
import com.mentorconnect.exception.DuplicateResourceException;
import com.mentorconnect.exception.InvalidOperationException;
import com.mentorconnect.exception.ResourceNotFoundException;
import com.mentorconnect.model.InterestTag;
import com.mentorconnect.model.PairStatus;
import com.mentorconnect.model.Student;
import com.mentorconnect.repository.MentorshipPairRepository;
import com.mentorconnect.repository.StudentRepository;
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
public class StudentService {

    private final StudentRepository studentRepository;
    private final MentorshipPairRepository mentorshipPairRepository;
    private final InterestTagService interestTagService;
    private final AuditLogService auditLogService;

    public StudentService(StudentRepository studentRepository,
                          MentorshipPairRepository mentorshipPairRepository,
                          InterestTagService interestTagService,
                          AuditLogService auditLogService) {
        this.studentRepository = studentRepository;
        this.mentorshipPairRepository = mentorshipPairRepository;
        this.interestTagService = interestTagService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<StudentResponseDto> getAllStudents() {
        return studentRepository.findAllWithTags().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentResponseDto getStudentById(Long id) {
        Student student = studentRepository.findByIdWithTags(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return toDto(student);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponseDto> getStudentsPaged(Pageable pageable) {
        return studentRepository.findAll(pageable).map(this::toDto);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public StudentResponseDto createStudent(StudentRequestDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (studentRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Student already exists with email: " + email);
        }

        Student student = new Student();
        student.setName(dto.getName().trim());
        student.setEmail(email);
        student.setDepartment(dto.getDepartment().trim());
        student.setBatchYear(dto.getBatchYear());
        student.setRollNumber(dto.getRollNumber() != null ? dto.getRollNumber().trim() : null);

        if (dto.getInterestTags() != null) {
            Set<InterestTag> tags = new HashSet<>();
            for (String tagName : dto.getInterestTags()) {
                if (tagName != null && !tagName.trim().isEmpty()) {
                    tags.add(interestTagService.getOrCreateTag(tagName.trim()));
                }
            }
            student.setInterestTags(tags);
        }

        student = studentRepository.save(student);
        auditLogService.recordLog(
                "STUDENT_REGISTERED",
                "STUDENT",
                student.getId(),
                "Registered student: " + student.getName() + " (" + student.getDepartment() + ", Batch " + student.getBatchYear() + ")"
        );
        return toDto(student);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public StudentResponseDto updateStudent(Long id, StudentRequestDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        String email = dto.getEmail().trim().toLowerCase();
        if (!student.getEmail().equalsIgnoreCase(email) && studentRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Another student already registered with email: " + email);
        }

        student.setName(dto.getName().trim());
        student.setEmail(email);
        student.setDepartment(dto.getDepartment().trim());
        student.setBatchYear(dto.getBatchYear());
        student.setRollNumber(dto.getRollNumber() != null ? dto.getRollNumber().trim() : null);

        if (dto.getInterestTags() != null) {
            Set<InterestTag> tags = new HashSet<>();
            for (String tagName : dto.getInterestTags()) {
                if (tagName != null && !tagName.trim().isEmpty()) {
                    tags.add(interestTagService.getOrCreateTag(tagName.trim()));
                }
            }
            student.setInterestTags(tags);
        }

        student = studentRepository.save(student);
        return toDto(student);
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        long totalPairs = mentorshipPairRepository.countByStudentId(id);
        if (totalPairs > 0) {
            throw new InvalidOperationException("Cannot delete student '" + student.getName() + "' because they have " + totalPairs + " mentorship pair record(s). Mentorship history must be preserved.");
        }

        student.getInterestTags().clear();
        studentRepository.save(student);
        studentRepository.delete(student);
    }

    public StudentResponseDto toDto(Student student) {
        StudentResponseDto dto = new StudentResponseDto();
        dto.setId(student.getId());
        dto.setName(student.getName());
        dto.setEmail(student.getEmail());
        dto.setDepartment(student.getDepartment());
        dto.setBatchYear(student.getBatchYear());
        dto.setRollNumber(student.getRollNumber());

        if (student.getInterestTags() != null) {
            dto.setInterestTags(student.getInterestTags().stream()
                    .map(interestTagService::toDto)
                    .collect(Collectors.toSet()));
        }

        List<com.mentorconnect.model.MentorshipPair> pairs = mentorshipPairRepository.findByStudentId(student.getId());
        int activeCount = (int) pairs.stream().filter(p -> p.getStatus() == PairStatus.ACTIVE).count();
        dto.setActiveMentorshipCount(activeCount);

        return dto;
    }
}
