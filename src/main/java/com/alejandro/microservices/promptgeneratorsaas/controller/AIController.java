package com.alejandro.microservices.promptgeneratorsaas.controller;

import com.alejandro.microservices.promptgeneratorsaas.dto.AIGenerationRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.AIGenerationResponse;
import com.alejandro.microservices.promptgeneratorsaas.service.AIProviderService;
import com.alejandro.microservices.promptgeneratorsaas.service.AIServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    private final AIServiceFactory aiServiceFactory;

    public AIController(AIServiceFactory aiServiceFactory) {
        this.aiServiceFactory = aiServiceFactory;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> generate(@RequestBody AIGenerationRequest request) {
        try {
            AIProviderService service = aiServiceFactory.getProvider(request.getProvider());
            
            if (service instanceof com.alejandro.microservices.promptgeneratorsaas.service.OpenAIService) {
                com.alejandro.microservices.promptgeneratorsaas.service.OpenAIService openAIService = 
                    (com.alejandro.microservices.promptgeneratorsaas.service.OpenAIService) service;
                AIGenerationResponse response = openAIService.generateResponseWithDetails(request.getPrompt(), request.getModel());
                return ResponseEntity.ok(response);
            } else if (service instanceof com.alejandro.microservices.promptgeneratorsaas.service.ClaudeService) {
                com.alejandro.microservices.promptgeneratorsaas.service.ClaudeService claudeService = 
                    (com.alejandro.microservices.promptgeneratorsaas.service.ClaudeService) service;
                AIGenerationResponse response = claudeService.generateResponseWithDetails(request.getPrompt(), request.getModel());
                return ResponseEntity.ok(response);
            } else if (service instanceof com.alejandro.microservices.promptgeneratorsaas.service.MockAIService) {
                com.alejandro.microservices.promptgeneratorsaas.service.MockAIService mockService = 
                    (com.alejandro.microservices.promptgeneratorsaas.service.MockAIService) service;
                AIGenerationResponse response = mockService.generateResponseWithDetails(request.getPrompt(), request.getModel());
                return ResponseEntity.ok(response);
            } else {
                // Fallback for simple response
                String response = service.generateResponse(request.getPrompt());
                return ResponseEntity.ok(new AIGenerationResponse(
                    response,
                    service.getProviderName(),
                    request.getModel(),
                    System.currentTimeMillis(),
                    true,
                    null
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error generating AI response: " + e.getMessage()));
        }
    }

    @PostMapping("/generate/simple")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> generateSimple(@RequestParam String provider, @RequestBody String prompt) {
        try {
            AIProviderService service = aiServiceFactory.getProvider(provider);
            String response = service.generateResponse(prompt);
            return ResponseEntity.ok(Map.of(
                "response", response,
                "provider", service.getProviderName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error generating AI response: " + e.getMessage()));
        }
    }

    @GetMapping("/providers")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<String>> getAvailableProviders() {
        return ResponseEntity.ok(aiServiceFactory.getAvailableProviders());
    }

    @GetMapping("/providers/{providerName}/status")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> checkProviderStatus(@PathVariable String providerName) {
        boolean isAvailable = aiServiceFactory.isProviderAvailable(providerName);
        return ResponseEntity.ok(Map.of(
            "provider", providerName,
            "available", isAvailable
        ));
    }
}
