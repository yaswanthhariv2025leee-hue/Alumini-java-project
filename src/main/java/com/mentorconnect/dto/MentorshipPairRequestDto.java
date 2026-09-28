package com.mentorconnect.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class MentorshipPairRequestDto {

    @NotNull(message = "Alumni ID is required")
    @Positive(message = "Alumni ID must be a positive number")
    private Long alumniId;

    @NotNull(message = "Student ID is required")
    @Positive(message = "Student ID must be a positive number")
    private Long studentId;

    private String notes;

    public MentorshipPairRequestDto() {
    }

    public MentorshipPairRequestDto(Long alumniId, Long studentId, String notes) {
        this.alumniId = alumniId;
        this.studentId = studentId;
        this.notes = notes;
    }

    public Long getAlumniId() {
        return alumniId;
    }

    public void setAlumniId(Long alumniId) {
        this.alumniId = alumniId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
