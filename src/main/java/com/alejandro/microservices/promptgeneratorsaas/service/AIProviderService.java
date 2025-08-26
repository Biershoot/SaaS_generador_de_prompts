package com.alejandro.microservices.promptgeneratorsaas.service;

public interface AIProviderService {
    String generateResponse(String prompt);
    String getProviderName();
}
