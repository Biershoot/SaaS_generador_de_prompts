package com.alejandro.microservices.promptgeneratorsaas.service;

import com.alejandro.microservices.promptgeneratorsaas.dto.AIGenerationResponse;
import org.springframework.stereotype.Service;

@Service
public class MockAIService implements AIProviderService {

    @Override
    public String generateResponse(String prompt) {
        // Simulate API delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return String.format("""
            [MOCK AI RESPONSE]
            
            Prompt: %s
            
            This is a mock response for development purposes. 
            In production, this would be replaced with actual AI provider responses.
            
            Generated content based on your prompt:
            - Creative suggestion 1
            - Creative suggestion 2
            - Creative suggestion 3
            
            [End of mock response]
            """, prompt);
    }

    @Override
    public String getProviderName() {
        return "mock";
    }

    public AIGenerationResponse generateResponseWithDetails(String prompt, String model) {
        try {
            String response = generateResponse(prompt);
            return new AIGenerationResponse(
                response,
                getProviderName(),
                model != null ? model : "mock-model",
                System.currentTimeMillis(),
                true,
                null
            );
        } catch (Exception e) {
            return new AIGenerationResponse(
                null,
                getProviderName(),
                model != null ? model : "mock-model",
                System.currentTimeMillis(),
                false,
                e.getMessage()
            );
        }
    }
}
