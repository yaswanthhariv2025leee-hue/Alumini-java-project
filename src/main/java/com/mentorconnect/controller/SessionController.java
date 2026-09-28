package com.mentorconnect.controller;

import com.mentorconnect.dto.SessionRequestDto;
import com.mentorconnect.dto.SessionResponseDto;
import com.mentorconnect.model.SessionStatus;
import com.mentorconnect.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ResponseEntity<List<SessionResponseDto>> getAllSessions(
            @RequestParam(required = false) Long pairId,
            @RequestParam(required = false) SessionStatus status) {
        return ResponseEntity.ok(sessionService.getAllSessions(pairId, status));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<SessionResponseDto>> getSessionsPaged(
            @RequestParam(required = false) Long pairId,
            @RequestParam(required = false) SessionStatus status,
            @PageableDefault(size = 10, sort = "sessionDate") Pageable pageable) {
        return ResponseEntity.ok(sessionService.getSessionsPaged(pageable, pairId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponseDto> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    @PostMapping
    public ResponseEntity<SessionResponseDto> scheduleSession(@Valid @RequestBody SessionRequestDto request) {
        return new ResponseEntity<>(sessionService.scheduleSession(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<SessionResponseDto> updateSessionStatus(
            @PathVariable Long id,
            @RequestParam SessionStatus status,
            @RequestParam(required = false) String feedbackNotes) {
        return ResponseEntity.ok(sessionService.updateSessionStatus(id, status, feedbackNotes));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionResponseDto> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionRequestDto request) {
        return ResponseEntity.ok(sessionService.updateSession(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
