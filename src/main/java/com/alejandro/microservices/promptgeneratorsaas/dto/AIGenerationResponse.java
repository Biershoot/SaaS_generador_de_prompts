package com.alejandro.microservices.promptgeneratorsaas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIGenerationResponse {
    private String response;
    private String provider;
    private String model;
    private long timestamp;
    private boolean success;
    private String error;
}
