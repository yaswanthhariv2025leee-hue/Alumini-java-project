package com.mentorconnect.controller;

import com.mentorconnect.dto.MatchSuggestionDto;
import com.mentorconnect.dto.MentorshipPairRequestDto;
import com.mentorconnect.dto.MentorshipPairResponseDto;
import com.mentorconnect.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matching")
@CrossOrigin(origins = "*")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/suggest/{studentId}")
    public ResponseEntity<List<MatchSuggestionDto>> suggestMentorsForStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(matchingService.suggestMentorsForStudent(studentId));
    }

    @PostMapping("/suggest-custom")
    public ResponseEntity<List<MatchSuggestionDto>> suggestMentorsByTags(@RequestBody List<String> tags) {
        return ResponseEntity.ok(matchingService.suggestMentorsByTags(tags));
    }

    @PostMapping("/pair")
    public ResponseEntity<MentorshipPairResponseDto> createPair(@Valid @RequestBody MentorshipPairRequestDto request) {
        return new ResponseEntity<>(matchingService.createPair(request), HttpStatus.CREATED);
    }
}
