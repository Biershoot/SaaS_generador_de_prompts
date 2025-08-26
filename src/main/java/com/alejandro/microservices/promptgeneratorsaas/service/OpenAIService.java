package com.alejandro.microservices.promptgeneratorsaas.service;

import com.alejandro.microservices.promptgeneratorsaas.dto.AIGenerationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class OpenAIService implements AIProviderService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;

    public OpenAIService(@Value("${openai.api.key:}") String apiKey,
                        @Value("${openai.model:gpt-4o-mini}") String defaultModel) {
        this.defaultModel = defaultModel;
        this.objectMapper = new ObjectMapper();
        
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String generateResponse(String prompt) {
        try {
            String model = defaultModel;
            
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", new Object[]{
                    Map.of("role", "user", "content", prompt)
                },
                "max_tokens", 1000,
                "temperature", 0.7
            );

            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse the response to extract the content
            JsonNode jsonResponse = objectMapper.readTree(response);
            return jsonResponse.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (WebClientResponseException e) {
            throw new RuntimeException("OpenAI API error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error calling OpenAI API", e);
        }
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    public AIGenerationResponse generateResponseWithDetails(String prompt, String model) {
        long startTime = System.currentTimeMillis();
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
