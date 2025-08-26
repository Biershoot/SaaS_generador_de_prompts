package com.alejandro.microservices.promptgeneratorsaas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIProviderConfig {
    
    private String defaultProvider = "mock";
    private boolean enableMockProvider = true;
    
    public String getDefaultProvider() {
        return defaultProvider;
    }
    
    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }
    
    public boolean isEnableMockProvider() {
        return enableMockProvider;
    }
    
    public void setEnableMockProvider(boolean enableMockProvider) {
        this.enableMockProvider = enableMockProvider;
    }
}
