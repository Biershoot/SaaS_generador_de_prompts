package com.alejandro.microservices.promptgeneratorsaas.service;

import com.alejandro.microservices.promptgeneratorsaas.dto.AIGenerationRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.AIGenerationResponse;
import com.alejandro.microservices.promptgeneratorsaas.entity.Prompt;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import com.alejandro.microservices.promptgeneratorsaas.repository.PromptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PromptGenerationService {

    private final AIServiceFactory aiServiceFactory;
    private final PromptRepository promptRepository;

    public PromptGenerationService(AIServiceFactory aiServiceFactory, PromptRepository promptRepository) {
        this.aiServiceFactory = aiServiceFactory;
        this.promptRepository = promptRepository;
    }

    /**
     * Genera un prompt usando el proveedor de IA especificado
     */
    public AIGenerationResponse generatePrompt(String providerName, String promptText, String model, User user) {
        try {
            AIProviderService provider = aiServiceFactory.getProvider(providerName);
            AIGenerationResponse response = provider.generateResponseWithDetails(promptText, model);
            
            // Guardar el prompt generado en la base de datos
            if (response.isSuccess()) {
                saveGeneratedPrompt(user, promptText, response.getResponse(), providerName, model, response.getResponse());
            }
            
            return response;
        } catch (Exception e) {
            return new AIGenerationResponse(
                null,
                providerName,
                model,
                System.currentTimeMillis(),
                false,
                e.getMessage()
            );
        }
    }

    /**
     * Genera múltiples variaciones de un prompt
     */
    public List<AIGenerationResponse> generatePromptVariations(String providerName, String basePrompt, 
                                                              String model, User user, int variations) {
        List<AIGenerationResponse> responses = new java.util.ArrayList<>();
        
        for (int i = 0; i < variations; i++) {
            String variationPrompt = basePrompt + " (variación " + (i + 1) + ")";
            AIGenerationResponse response = generatePrompt(providerName, variationPrompt, model, user);
            responses.add(response);
        }
        
        return responses;
    }

    /**
     * Genera un prompt optimizado para una categoría específica
     */
    public AIGenerationResponse generateOptimizedPrompt(String providerName, String basePrompt, 
                                                       String category, String model, User user) {
        String optimizedPrompt = String.format("""
            Genera un prompt optimizado para la categoría '%s' basado en la siguiente descripción:
            
            %s
            
            El prompt debe ser específico, detallado y optimizado para obtener los mejores resultados en esta categoría.
            """, category, basePrompt);
        
        return generatePrompt(providerName, optimizedPrompt, model, user);
    }

    /**
     * Genera un prompt con parámetros específicos
     */
    public AIGenerationResponse generatePromptWithParameters(String providerName, String basePrompt, 
                                                           Map<String, Object> parameters, String model, User user) {
        StringBuilder enhancedPrompt = new StringBuilder(basePrompt);
        enhancedPrompt.append("\n\nParámetros específicos:\n");
        
        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            enhancedPrompt.append("- ").append(param.getKey()).append(": ").append(param.getValue()).append("\n");
        }
        
        return generatePrompt(providerName, enhancedPrompt.toString(), model, user);
    }

    /**
     * Guarda el prompt generado en la base de datos
     */
    private void saveGeneratedPrompt(User user, String originalPrompt, String generatedContent, 
                                   String provider, String model, String content) {
        Prompt prompt = Prompt.builder()
                .user(user)
                .title("Prompt generado con " + provider)
                .content(content)
                .category("ai-generated")
                .build();
        
        promptRepository.save(prompt);
    }

    /**
     * Obtiene el historial de prompts generados por un usuario
     */
    public List<Prompt> getUserGeneratedPrompts(User user) {
        return promptRepository.findByUser(user);
    }

    /**
     * Obtiene prompts por categoría
     */
    public List<Prompt> getPromptsByCategory(User user, String category) {
        return promptRepository.findByUserAndCategory(user, category);
    }

    /**
     * Busca prompts en el historial del usuario
     */
    public List<Prompt> searchUserPrompts(User user, String searchTerm) {
        return promptRepository.searchUserPrompts(user, searchTerm);
    }

    /**
     * Verifica si un proveedor está disponible
     */
    public boolean isProviderAvailable(String providerName) {
        try {
            AIProviderService provider = aiServiceFactory.getProvider(providerName);
            return provider.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene los modelos soportados por un proveedor
     */
    public String[] getProviderModels(String providerName) {
        try {
            AIProviderService provider = aiServiceFactory.getProvider(providerName);
            return provider.getSupportedModels();
        } catch (Exception e) {
            return new String[0];
        }
    }
}
