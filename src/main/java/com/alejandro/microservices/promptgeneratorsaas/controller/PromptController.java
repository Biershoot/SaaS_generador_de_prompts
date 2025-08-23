package com.alejandro.microservices.promptgeneratorsaas.controller;

import com.alejandro.microservices.promptgeneratorsaas.dto.CreatePromptRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.PromptDto;
import com.alejandro.microservices.promptgeneratorsaas.service.PromptService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/prompts")
@CrossOrigin(origins = "*")
public class PromptController {
    
    @Autowired
    private PromptService promptService;
    
    @PostMapping
    public ResponseEntity<PromptDto> createPrompt(
            @Valid @RequestBody CreatePromptRequest request,
            @RequestHeader("X-User-ID") Long userId) {
        PromptDto createdPrompt = promptService.createPrompt(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPrompt);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PromptDto> getPromptById(@PathVariable Long id) {
        Optional<PromptDto> prompt = promptService.getPromptById(id);
        return prompt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PromptDto>> getUserPrompts(@PathVariable Long userId) {
        List<PromptDto> prompts = promptService.getUserPrompts(userId);
        return ResponseEntity.ok(prompts);
    }
    
    @GetMapping
    public ResponseEntity<List<PromptDto>> getAllPrompts() {
        List<PromptDto> prompts = promptService.getAllPrompts();
        return ResponseEntity.ok(prompts);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<PromptDto>> searchPrompts(
            @RequestParam String q) {
        List<PromptDto> prompts = promptService.searchPrompts(q);
        return ResponseEntity.ok(prompts);
    }
    
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<List<PromptDto>> searchUserPrompts(
            @PathVariable Long userId,
            @RequestParam String q) {
        List<PromptDto> prompts = promptService.searchUserPrompts(userId, q);
        return ResponseEntity.ok(prompts);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PromptDto> updatePrompt(
            @PathVariable Long id,
            @Valid @RequestBody CreatePromptRequest request,
            @RequestHeader("X-User-ID") Long userId) {
        Optional<PromptDto> updatedPrompt = promptService.updatePrompt(id, request, userId);
        return updatedPrompt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(
            @PathVariable Long id,
            @RequestHeader("X-User-ID") Long userId) {
        boolean deleted = promptService.deletePrompt(id, userId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
