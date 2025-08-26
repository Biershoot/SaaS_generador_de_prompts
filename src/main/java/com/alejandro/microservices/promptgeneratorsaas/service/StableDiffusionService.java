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
public class StableDiffusionService implements AIProviderService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;

    public StableDiffusionService(@Value("${stable-diffusion.api.url:https://api.stability.ai}") String apiUrl,
                                 @Value("${stable-diffusion.api.key:}") String apiKey,
                                 @Value("${stable-diffusion.model:stable-diffusion-xl-1024-v1-0}") String defaultModel) {
        this.defaultModel = defaultModel;
        this.objectMapper = new ObjectMapper();
        
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String generateResponse(String prompt) {
        return generateResponse(prompt, defaultModel);
    }

    @Override
    public String generateResponse(String prompt, String model) {
        try {
            String modelToUse = model != null ? model : defaultModel;
            
            Map<String, Object> requestBody = Map.of(
                "text_prompts", new Object[]{
                    Map.of("text", prompt, "weight", 1.0)
                },
                "cfg_scale", 7,
                "height", 1024,
                "width", 1024,
                "samples", 1,
                "steps", 30
            );

            String response = webClient.post()
                    .uri("/v1/generation/" + modelToUse + "/text-to-image")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse the response to extract the image URL
            JsonNode jsonResponse = objectMapper.readTree(response);
            return jsonResponse.path("artifacts")
                    .path(0)
                    .path("base64")
                    .asText();

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Stable Diffusion API error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Stable Diffusion API", e);
        }
    }

    @Override
    public AIGenerationResponse generateResponseWithDetails(String prompt, String model) {
        try {
            String response = generateResponse(prompt, model);
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

    @Override
    public String getProviderName() {
        return "stable-diffusion";
    }

    @Override
    public boolean isAvailable() {
        try {
            // Simple health check
            return webClient != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String[] getSupportedModels() {
        return new String[]{
            "stable-diffusion-xl-1024-v1-0",
            "stable-diffusion-v1-6",
            "stable-diffusion-512-v2-1",
            "stable-diffusion-768-v2-1"
        };
    }
}
