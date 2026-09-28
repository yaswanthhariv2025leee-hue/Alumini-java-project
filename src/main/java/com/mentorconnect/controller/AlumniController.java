package com.mentorconnect.controller;

import com.mentorconnect.dto.AlumniRequestDto;
import com.mentorconnect.dto.AlumniResponseDto;
import com.mentorconnect.service.AlumniService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alumni")
@CrossOrigin(origins = "*")
public class AlumniController {

    private final AlumniService alumniService;

    public AlumniController(AlumniService alumniService) {
        this.alumniService = alumniService;
    }

    @GetMapping
    public ResponseEntity<List<AlumniResponseDto>> getAllAlumni(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag) {
        return ResponseEntity.ok(alumniService.getAllAlumni(search, tag));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<AlumniResponseDto>> getAlumniPaged(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(alumniService.getAlumniPaged(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumniResponseDto> getAlumniById(@PathVariable Long id) {
        return ResponseEntity.ok(alumniService.getAlumniById(id));
    }

    @PostMapping
    public ResponseEntity<AlumniResponseDto> createAlumni(@Valid @RequestBody AlumniRequestDto dto) {
        return new ResponseEntity<>(alumniService.createAlumni(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumniResponseDto> updateAlumni(
            @PathVariable Long id,
            @Valid @RequestBody AlumniRequestDto dto) {
        return ResponseEntity.ok(alumniService.updateAlumni(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlumni(@PathVariable Long id) {
        alumniService.deleteAlumni(id);
        return ResponseEntity.noContent().build();
    }
}
