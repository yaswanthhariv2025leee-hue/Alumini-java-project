package com.mentorconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "alumni")
public class Alumni {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotNull(message = "Graduation year is required")
    @Min(value = 1950, message = "Graduation year must be valid")
    @Column(name = "graduation_year", nullable = false)
    private Integer graduationYear;

    @NotBlank(message = "Company is required")
    @Column(nullable = false, length = 150)
    private String company;

    @NotBlank(message = "Job title is required")
    @Column(name = "job_title", nullable = false, length = 150)
    private String jobTitle;

    @NotNull(message = "Maximum concurrent mentee limit is required")
    @Min(value = 1, message = "Maximum concurrent mentee limit must be at least 1")
    @Column(name = "max_concurrent_mentees", nullable = false)
    private Integer maxConcurrentMentees;

    @Column(length = 1000)
    private String bio;

    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "alumni_interest_tags",
            joinColumns = @JoinColumn(name = "alumni_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<InterestTag> expertiseTags = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "alumni", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MentorshipPair> mentorshipPairs = new ArrayList<>();

    public Alumni() {
    }

    public Alumni(String name, String email, Integer graduationYear, String company, String jobTitle, Integer maxConcurrentMentees) {
        this.name = name;
        this.email = email;
        this.graduationYear = graduationYear;
        this.company = company;
        this.jobTitle = jobTitle;
        this.maxConcurrentMentees = maxConcurrentMentees;
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

    public Set<InterestTag> getExpertiseTags() {
        return expertiseTags;
    }

    public void setExpertiseTags(Set<InterestTag> expertiseTags) {
        this.expertiseTags = expertiseTags;
    }

    public List<MentorshipPair> getMentorshipPairs() {
        return mentorshipPairs;
    }

    public void setMentorshipPairs(List<MentorshipPair> mentorshipPairs) {
        this.mentorshipPairs = mentorshipPairs;
    }

    public int getActiveMenteeCount() {
        if (mentorshipPairs == null) return 0;
        return (int) mentorshipPairs.stream()
                .filter(p -> p.getStatus() == PairStatus.ACTIVE)
                .count();
    }

    public int getAvailableSlots() {
        int max = maxConcurrentMentees != null ? maxConcurrentMentees : 0;
        return Math.max(0, max - getActiveMenteeCount());
    }
}
