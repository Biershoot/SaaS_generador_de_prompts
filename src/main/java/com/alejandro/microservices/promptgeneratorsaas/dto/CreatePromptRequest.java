package com.alejandro.microservices.promptgeneratorsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePromptRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be less than 100 characters")
    private String title;
    
    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must be less than 10000 characters")
    private String content;
    
    // Constructors
    public CreatePromptRequest() {}
    
    public CreatePromptRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }
    
    // Getters and Setters
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
}
