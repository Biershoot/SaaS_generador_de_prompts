package com.alejandro.microservices.promptgeneratorsaas.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefreshTokenService {
    
    // In-memory storage for refresh tokens (in production, use Redis or database)
    private final Map<String, String> refreshTokens = new ConcurrentHashMap<>();
    
    public void save(String username, String refreshToken) {
        refreshTokens.put(username, refreshToken);
    }
    
    public boolean isValid(String username, String refreshToken) {
        String storedToken = refreshTokens.get(username);
        return storedToken != null && storedToken.equals(refreshToken);
    }
    
    public void revoke(String refreshToken) {
        // Find and remove the token
        refreshTokens.entrySet().removeIf(entry -> entry.getValue().equals(refreshToken));
    }
    
    public void revokeByUsername(String username) {
        refreshTokens.remove(username);
    }
}
