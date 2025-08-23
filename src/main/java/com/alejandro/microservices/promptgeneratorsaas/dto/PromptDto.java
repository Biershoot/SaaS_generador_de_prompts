package com.alejandro.microservices.promptgeneratorsaas.dto;

import java.time.LocalDateTime;

public class PromptDto {
    
    private Long id;
    private String title;
    private String content;
    private String category;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
    
    // Constructors
    public PromptDto() {}
    
    public PromptDto(String title, String content, String category, Long userId) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.userId = userId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
