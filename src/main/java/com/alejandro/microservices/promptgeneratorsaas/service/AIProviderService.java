package com.alejandro.microservices.promptgeneratorsaas.service;

import com.alejandro.microservices.promptgeneratorsaas.dto.AIGenerationResponse;

public interface AIProviderService {
    String generateResponse(String prompt);
    String generateResponse(String prompt, String model);
    AIGenerationResponse generateResponseWithDetails(String prompt, String model);
    String getProviderName();
    boolean isAvailable();
    String[] getSupportedModels();
}
