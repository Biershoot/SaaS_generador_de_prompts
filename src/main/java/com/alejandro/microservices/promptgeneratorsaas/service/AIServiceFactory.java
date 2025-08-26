package com.alejandro.microservices.promptgeneratorsaas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AIServiceFactory {

    private final Map<String, AIProviderService> providers;

    public AIServiceFactory(@Autowired List<AIProviderService> aiServices) {
        this.providers = Map.of(
            "openai", aiServices.stream().filter(s -> s.getProviderName().equals("openai")).findFirst().orElse(null),
            "claude", aiServices.stream().filter(s -> s.getProviderName().equals("claude")).findFirst().orElse(null),
            "mock", aiServices.stream().filter(s -> s.getProviderName().equals("mock")).findFirst().orElse(null)
        );
    }

    public AIProviderService getProvider(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty");
        }

        AIProviderService provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("Provider not supported: " + providerName + 
                ". Available providers: " + String.join(", ", providers.keySet()));
        }

        return provider;
    }

    public List<String> getAvailableProviders() {
        return List.copyOf(providers.keySet());
    }

    public boolean isProviderAvailable(String providerName) {
        return providers.containsKey(providerName.toLowerCase());
    }
}
