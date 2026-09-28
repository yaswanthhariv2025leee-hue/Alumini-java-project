package com.mentorconnect.dto;

import java.util.HashSet;
import java.util.Set;

public class AlumniResponseDto {
    private Long id;
    private String name;
    private String email;
    private Integer graduationYear;
    private String company;
    private String jobTitle;
    private Integer maxConcurrentMentees;
    private String bio;
    private String linkedinUrl;
    private Set<InterestTagDto> expertiseTags = new HashSet<>();
    private int activeMenteeCount;
    private int availableSlots;

    public AlumniResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public Integer getMaxConcurrentMentees() {
        return maxConcurrentMentees;
    }

    public void setMaxConcurrentMentees(Integer maxConcurrentMentees) {
        this.maxConcurrentMentees = maxConcurrentMentees;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public Set<InterestTagDto> getExpertiseTags() {
        return expertiseTags;
    }

    public void setExpertiseTags(Set<InterestTagDto> expertiseTags) {
        this.expertiseTags = expertiseTags;
    }

    public int getActiveMenteeCount() {
        return activeMenteeCount;
    }

    public void setActiveMenteeCount(int activeMenteeCount) {
        this.activeMenteeCount = activeMenteeCount;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }
}
