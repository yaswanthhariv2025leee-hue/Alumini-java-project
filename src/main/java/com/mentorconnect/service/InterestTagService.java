package com.mentorconnect.service;

import com.mentorconnect.dto.InterestTagDto;
import com.mentorconnect.exception.DuplicateResourceException;
import com.mentorconnect.exception.InvalidOperationException;
import com.mentorconnect.exception.ResourceNotFoundException;
import com.mentorconnect.model.InterestTag;
import com.mentorconnect.repository.AlumniRepository;
import com.mentorconnect.repository.InterestTagRepository;
import com.mentorconnect.repository.StudentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InterestTagService {

    private final InterestTagRepository interestTagRepository;
    private final AlumniRepository alumniRepository;
    private final StudentRepository studentRepository;

    public InterestTagService(InterestTagRepository interestTagRepository,
                              AlumniRepository alumniRepository,
                              StudentRepository studentRepository) {
        this.interestTagRepository = interestTagRepository;
        this.alumniRepository = alumniRepository;
        this.studentRepository = studentRepository;
    }

    @Cacheable("tags")
    @Transactional(readOnly = true)
    public List<InterestTagDto> getAllTags() {
        return interestTagRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "tags", allEntries = true)
    public InterestTagDto createTag(InterestTagDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty");
        }
        String cleanName = dto.getName().trim();
        if (interestTagRepository.existsByNameIgnoreCase(cleanName)) {
            throw new DuplicateResourceException("Interest tag '" + cleanName + "' already exists");
        }
        InterestTag tag = new InterestTag(cleanName, dto.getCategory());
        tag = interestTagRepository.save(tag);
        return toDto(tag);
    }

    @CacheEvict(value = "tags", allEntries = true)
    public InterestTag getOrCreateTag(String name) {
        String cleanName = name.trim();
        return interestTagRepository.findByNameIgnoreCase(cleanName)
                .orElseGet(() -> interestTagRepository.save(new InterestTag(cleanName, "General")));
    }

    @CacheEvict(value = "tags", allEntries = true)
    public void deleteTag(Long id) {
        InterestTag tag = interestTagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interest tag not found with id: " + id));

        long alumniCount = alumniRepository.countAlumniWithTag(id);
        long studentCount = studentRepository.countStudentsWithTag(id);
        if (alumniCount > 0 || studentCount > 0) {
            throw new InvalidOperationException("Cannot delete interest tag '" + tag.getName() + "' because it is currently assigned to " + alumniCount + " alumni and " + studentCount + " student(s).");
        }

        interestTagRepository.delete(tag);
    }

    public InterestTagDto toDto(InterestTag tag) {
        return new InterestTagDto(tag.getId(), tag.getName(), tag.getCategory());
    }
}
