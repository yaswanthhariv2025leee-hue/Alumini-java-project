package com.mentorconnect.dto;

import jakarta.validation.constraints.NotBlank;

public class InterestTagDto {
    private Long id;

    @NotBlank(message = "Tag name cannot be blank")
    private String name;
    private String category;

    public InterestTagDto() {
    }

    public InterestTagDto(Long id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
