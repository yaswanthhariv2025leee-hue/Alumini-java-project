package com.mentorconnect.controller;

import com.mentorconnect.dto.InterestTagDto;
import com.mentorconnect.service.InterestTagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = "*")
public class InterestTagController {

    private final InterestTagService interestTagService;

    public InterestTagController(InterestTagService interestTagService) {
        this.interestTagService = interestTagService;
    }

    @GetMapping
    public ResponseEntity<List<InterestTagDto>> getAllTags() {
        return ResponseEntity.ok(interestTagService.getAllTags());
    }

    @PostMapping
    public ResponseEntity<InterestTagDto> createTag(@Valid @RequestBody InterestTagDto dto) {
        return new ResponseEntity<>(interestTagService.createTag(dto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        interestTagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
