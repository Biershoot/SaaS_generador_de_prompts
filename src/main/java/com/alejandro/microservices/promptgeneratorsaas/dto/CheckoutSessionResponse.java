package com.alejandro.microservices.promptgeneratorsaas.dto;

import lombok.Data;

@Data
public class CheckoutSessionResponse {
    private String sessionId;
    private String sessionUrl;
    private String message;
}
