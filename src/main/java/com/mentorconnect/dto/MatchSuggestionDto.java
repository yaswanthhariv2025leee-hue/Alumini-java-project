package com.mentorconnect.dto;

import java.util.ArrayList;
import java.util.List;

public class MatchSuggestionDto {
    private Long alumniId;
    private String alumniName;
    private String email;
    private String company;
    private String jobTitle;
    private Integer graduationYear;
    private Integer maxConcurrentMentees;
    private int activeMentees;
    private int availableSlots;
    private boolean eligible;
    private boolean alreadyMatched;
    private List<String> matchingTags = new ArrayList<>();
    private int matchCount;
    private double matchPercentage;
    private List<String> allExpertiseTags = new ArrayList<>();

    public MatchSuggestionDto() {
    }

    public Long getAlumniId() {
        return alumniId;
    }

    public void setAlumniId(Long alumniId) {
        this.alumniId = alumniId;
    }

    public String getAlumniName() {
        return alumniName;
    }

    public void setAlumniName(String alumniName) {
        this.alumniName = alumniName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public Integer getMaxConcurrentMentees() {
        return maxConcurrentMentees;
    }

    public void setMaxConcurrentMentees(Integer maxConcurrentMentees) {
        this.maxConcurrentMentees = maxConcurrentMentees;
    }

    public int getActiveMentees() {
        return activeMentees;
    }

    public void setActiveMentees(int activeMentees) {
        this.activeMentees = activeMentees;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public List<String> getMatchingTags() {
        return matchingTags;
    }

    public void setMatchingTags(List<String> matchingTags) {
        this.matchingTags = matchingTags;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }

    public double getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(double matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public List<String> getAllExpertiseTags() {
        return allExpertiseTags;
    }

    public void setAllExpertiseTags(List<String> allExpertiseTags) {
        this.allExpertiseTags = allExpertiseTags;
    }

    public boolean isAlreadyMatched() {
        return alreadyMatched;
    }

    public void setAlreadyMatched(boolean alreadyMatched) {
        this.alreadyMatched = alreadyMatched;
    }
}
