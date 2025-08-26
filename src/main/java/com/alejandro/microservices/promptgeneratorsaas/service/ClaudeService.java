package com.alejandro.microservices.promptgeneratorsaas.service;

import com.alejandro.microservices.promptgeneratorsaas.dto.AIGenerationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
public class ClaudeService implements AIProviderService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;

    public ClaudeService(@Value("${claude.api.key:}") String apiKey,
                        @Value("${claude.model:claude-3-opus-20240229}") String defaultModel) {
        this.defaultModel = defaultModel;
        this.objectMapper = new ObjectMapper();
        
        this.webClient = WebClient.builder()
                .baseUrl("https://api.anthropic.com/v1")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    @Override
    public String generateResponse(String prompt) {
        try {
            String model = defaultModel;
            
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 1000,
                "messages", new Object[]{
                    Map.of("role", "user", "content", prompt)
                }
            );

            String response = webClient.post()
                    .uri("/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse the response to extract the content
            JsonNode jsonResponse = objectMapper.readTree(response);
            return jsonResponse.path("content")
                    .path(0)
                    .path("text")
                    .asText();

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Claude API error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Claude API", e);
        }
    }

    @Override
    public String getProviderName() {
        return "claude";
    }

    public AIGenerationResponse generateResponseWithDetails(String prompt, String model) {
        try {
            String response = generateResponse(prompt);
            return new AIGenerationResponse(
                response,
                getProviderName(),
                model != null ? model : defaultModel,
                System.currentTimeMillis(),
                true,
                null
            );
        } catch (Exception e) {
            return new AIGenerationResponse(
                null,
                getProviderName(),
                model != null ? model : defaultModel,
                System.currentTimeMillis(),
                false,
                e.getMessage()
            );
        }
    }
}
