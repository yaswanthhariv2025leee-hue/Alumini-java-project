package com.mentorconnect.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public class SessionRequestDto {

    @NotNull(message = "Mentorship pair ID is required")
    @Positive(message = "Mentorship pair ID must be a positive number")
    private Long mentorshipPairId;

    @NotNull(message = "Session date and time is required")
    private LocalDateTime sessionDate;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    private Integer durationMinutes;

    @NotBlank(message = "Topic is required")
    private String topic;

    private String meetingLink;
    private String feedbackNotes;

    public SessionRequestDto() {
    }

    public Long getMentorshipPairId() {
        return mentorshipPairId;
    }

    public void setMentorshipPairId(Long mentorshipPairId) {
        this.mentorshipPairId = mentorshipPairId;
    }

    public LocalDateTime getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDateTime sessionDate) {
        this.sessionDate = sessionDate;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getFeedbackNotes() {
        return feedbackNotes;
    }

    public void setFeedbackNotes(String feedbackNotes) {
        this.feedbackNotes = feedbackNotes;
    }
}
