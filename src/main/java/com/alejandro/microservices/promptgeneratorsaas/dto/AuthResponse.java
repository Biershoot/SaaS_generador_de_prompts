package com.alejandro.microservices.promptgeneratorsaas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private boolean success;
    private String token;
    private UserDto user;
    private String message;

    // Constructor para respuestas exitosas
    public static AuthResponse success(String token, UserDto user, String message) {
        return AuthResponse.builder()
                .success(true)
                .token(token)
                .user(user)
                .message(message)
                .build();
    }

    // Constructor para respuestas de error
    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserDto {
        private String id;
        private String email;
        private String name;
        private String role;
    }
}
