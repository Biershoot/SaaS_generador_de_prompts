package com.alejandro.microservices.promptgeneratorsaas.service;

import com.alejandro.microservices.promptgeneratorsaas.dto.CreatePromptRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.PromptDto;
import com.alejandro.microservices.promptgeneratorsaas.entity.Prompt;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import com.alejandro.microservices.promptgeneratorsaas.repository.PromptRepository;
import com.alejandro.microservices.promptgeneratorsaas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PromptService {
    
    @Autowired
    private PromptRepository promptRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public PromptDto createPrompt(CreatePromptRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Prompt prompt = new Prompt();
        prompt.setTitle(request.getTitle());
        prompt.setContent(request.getContent());
        prompt.setCategory(request.getCategory());
        prompt.setUser(user);
        
        Prompt savedPrompt = promptRepository.save(prompt);
        return convertToDto(savedPrompt);
    }
    
    public Optional<PromptDto> getPromptById(Long id) {
        return promptRepository.findById(id)
                .map(this::convertToDto);
    }
    
    public List<PromptDto> getUserPrompts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return promptRepository.findByUser(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<PromptDto> getAllPrompts() {
        return promptRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<PromptDto> searchPrompts(String searchTerm) {
        return promptRepository.searchPrompts(searchTerm)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<PromptDto> searchUserPrompts(Long userId, String searchTerm) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return promptRepository.searchUserPrompts(user, searchTerm)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<PromptDto> getPromptsByCategory(String category) {
        return promptRepository.findByCategory(category)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<PromptDto> getUserPromptsByCategory(Long userId, String category) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return promptRepository.findByUserAndCategory(user, category)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Optional<PromptDto> updatePrompt(Long id, CreatePromptRequest request, Long userId) {
                return promptRepository.findById(id)
                .filter(prompt -> prompt.getUser().getId().equals(userId))
                .map(prompt -> {
                        prompt.setTitle(request.getTitle());
                        prompt.setContent(request.getContent());
                        prompt.setCategory(request.getCategory());
                        return convertToDto(promptRepository.save(prompt));
                });
    }
    
    public boolean deletePrompt(Long id, Long userId) {
        Optional<Prompt> prompt = promptRepository.findById(id);
        if (prompt.isPresent() && prompt.get().getUser().getId().equals(userId)) {
            promptRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
        private PromptDto convertToDto(Prompt prompt) {
            PromptDto dto = new PromptDto();
            dto.setId(prompt.getId());
            dto.setTitle(prompt.getTitle());
            dto.setContent(prompt.getContent());
            dto.setCategory(prompt.getCategory());
            dto.setUserId(prompt.getUser().getId());
            dto.setUsername(prompt.getUser().getUsername());
            dto.setCreatedAt(prompt.getCreatedAt());
            return dto;
    }
}
