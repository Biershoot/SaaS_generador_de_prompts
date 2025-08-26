package com.alejandro.microservices.promptgeneratorsaas.controller;

import com.alejandro.microservices.promptgeneratorsaas.dto.AIGenerationRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.AIGenerationResponse;
import com.alejandro.microservices.promptgeneratorsaas.entity.Prompt;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import com.alejandro.microservices.promptgeneratorsaas.repository.UserRepository;
import com.alejandro.microservices.promptgeneratorsaas.service.PromptGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prompts")
@CrossOrigin(origins = "*")
public class PromptController {

    private final PromptGenerationService promptGenerationService;
    private final UserRepository userRepository;

    public PromptController(PromptGenerationService promptGenerationService, UserRepository userRepository) {
        this.promptGenerationService = promptGenerationService;
        this.userRepository = userRepository;
    }

    /**
     * Genera un prompt usando IA
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> generatePrompt(@RequestBody AIGenerationRequest request) {
        try {
            User user = getCurrentUser();
            AIGenerationResponse response = promptGenerationService.generatePrompt(
                request.getProvider(), 
                request.getPrompt(), 
                request.getModel(), 
                user
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Genera múltiples variaciones de un prompt
     */
    @PostMapping("/generate/variations")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> generatePromptVariations(@RequestBody AIGenerationRequest request, 
                                                    @RequestParam(defaultValue = "3") int variations) {
        try {
            User user = getCurrentUser();
            List<AIGenerationResponse> responses = promptGenerationService.generatePromptVariations(
                request.getProvider(), 
                request.getPrompt(), 
                request.getModel(), 
                user, 
                variations
            );
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Genera un prompt optimizado para una categoría
     */
    @PostMapping("/generate/optimized")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> generateOptimizedPrompt(@RequestBody AIGenerationRequest request, 
                                                   @RequestParam String category) {
        try {
            User user = getCurrentUser();
            AIGenerationResponse response = promptGenerationService.generateOptimizedPrompt(
                request.getProvider(), 
                request.getPrompt(), 
                category, 
                request.getModel(), 
                user
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Genera un prompt con parámetros específicos
     */
    @PostMapping("/generate/with-parameters")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> generatePromptWithParameters(@RequestBody AIGenerationRequest request, 
                                                        @RequestBody Map<String, Object> parameters) {
        try {
            User user = getCurrentUser();
            AIGenerationResponse response = promptGenerationService.generatePromptWithParameters(
                request.getProvider(), 
                request.getPrompt(), 
                parameters, 
                request.getModel(), 
                user
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtiene el historial de prompts del usuario
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Prompt>> getUserPrompts() {
        try {
            User user = getCurrentUser();
            List<Prompt> prompts = promptGenerationService.getUserGeneratedPrompts(user);
            return ResponseEntity.ok(prompts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene prompts por categoría
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Prompt>> getPromptsByCategory(@PathVariable String category) {
        try {
            User user = getCurrentUser();
            List<Prompt> prompts = promptGenerationService.getPromptsByCategory(user, category);
            return ResponseEntity.ok(prompts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Busca prompts en el historial del usuario
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Prompt>> searchPrompts(@RequestParam String query) {
        try {
            User user = getCurrentUser();
            List<Prompt> prompts = promptGenerationService.searchUserPrompts(user, query);
            return ResponseEntity.ok(prompts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Verifica si un proveedor está disponible
     */
    @GetMapping("/providers/{providerName}/status")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> checkProviderStatus(@PathVariable String providerName) {
        try {
            boolean isAvailable = promptGenerationService.isProviderAvailable(providerName);
            return ResponseEntity.ok(Map.of(
                "provider", providerName,
                "available", isAvailable
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtiene los modelos soportados por un proveedor
     */
    @GetMapping("/providers/{providerName}/models")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<String[]> getProviderModels(@PathVariable String providerName) {
        try {
            String[] models = promptGenerationService.getProviderModels(providerName);
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene el usuario actual autenticado
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
