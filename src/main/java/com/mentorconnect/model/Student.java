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
@Table(name = "students")
public class Student {

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

    @NotBlank(message = "Department is required")
    @Column(nullable = false, length = 100)
    private String department;

    @NotNull(message = "Batch year is required")
    @Min(value = 2000, message = "Batch year must be valid")
    @Column(name = "batch_year", nullable = false)
    private Integer batchYear;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "student_interest_tags",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<InterestTag> interestTags = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MentorshipPair> mentorshipPairs = new ArrayList<>();

    public Student() {
    }

    public Student(String name, String email, String department, Integer batchYear, String rollNumber) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.batchYear = batchYear;
        this.rollNumber = rollNumber;
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

    public Set<InterestTag> getInterestTags() {
        return interestTags;
    }

    public void setInterestTags(Set<InterestTag> interestTags) {
        this.interestTags = interestTags;
    }

    public List<MentorshipPair> getMentorshipPairs() {
        return mentorshipPairs;
    }

    public void setMentorshipPairs(List<MentorshipPair> mentorshipPairs) {
        this.mentorshipPairs = mentorshipPairs;
    }
}
