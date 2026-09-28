package com.mentorconnect.controller;

import com.mentorconnect.dto.MentorshipPairResponseDto;
import com.mentorconnect.model.PairStatus;
import com.mentorconnect.service.MentorshipPairService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mentorship-pairs")
@CrossOrigin(origins = "*")
public class MentorshipPairController {

    private final MentorshipPairService mentorshipPairService;

    public MentorshipPairController(MentorshipPairService mentorshipPairService) {
        this.mentorshipPairService = mentorshipPairService;
    }

    @GetMapping
    public ResponseEntity<List<MentorshipPairResponseDto>> getAllPairs(
            @RequestParam(required = false) PairStatus status,
            @RequestParam(required = false) Long alumniId,
            @RequestParam(required = false) Long studentId) {
        return ResponseEntity.ok(mentorshipPairService.getAllPairs(status, alumniId, studentId));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<MentorshipPairResponseDto>> getPairsPaged(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(mentorshipPairService.getPairsPaged(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MentorshipPairResponseDto> getPairById(@PathVariable Long id) {
        return ResponseEntity.ok(mentorshipPairService.getPairById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<MentorshipPairResponseDto> updatePairStatus(
            @PathVariable Long id,
            @RequestParam PairStatus status) {
        return ResponseEntity.ok(mentorshipPairService.updatePairStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePair(@PathVariable Long id) {
        mentorshipPairService.deletePair(id);
        return ResponseEntity.noContent().build();
    }
}
