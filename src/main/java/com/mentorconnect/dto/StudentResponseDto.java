package com.mentorconnect.dto;

import java.util.HashSet;
import java.util.Set;

public class StudentResponseDto {
    private Long id;
    private String name;
    private String email;
    private String department;
    private Integer batchYear;
    private String rollNumber;
    private Set<InterestTagDto> interestTags = new HashSet<>();
    private int activeMentorshipCount;

    public StudentResponseDto() {
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getBatchYear() {
        return batchYear;
    }

    public void setBatchYear(Integer batchYear) {
        this.batchYear = batchYear;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public Set<InterestTagDto> getInterestTags() {
        return interestTags;
    }

    public void setInterestTags(Set<InterestTagDto> interestTags) {
        this.interestTags = interestTags;
    }

    public int getActiveMentorshipCount() {
        return activeMentorshipCount;
    }

    public void setActiveMentorshipCount(int activeMentorshipCount) {
        this.activeMentorshipCount = activeMentorshipCount;
    }
}
