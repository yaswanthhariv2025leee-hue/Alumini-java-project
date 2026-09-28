package com.mentorconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

public class AlumniRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotNull(message = "Graduation year is required")
    @Min(value = 1950, message = "Graduation year must be valid")
    private Integer graduationYear;

    @NotBlank(message = "Company is required")
    private String company;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotNull(message = "Maximum concurrent mentee limit is required")
    @Min(value = 1, message = "Maximum concurrent mentee limit must be at least 1")
    private Integer maxConcurrentMentees;

    private String bio;
    private String linkedinUrl;
    private Set<String> expertiseTags = new HashSet<>();

    public AlumniRequestDto() {
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

    public Set<String> getExpertiseTags() {
        return expertiseTags;
    }

    public void setExpertiseTags(Set<String> expertiseTags) {
        this.expertiseTags = expertiseTags;
    }
}
