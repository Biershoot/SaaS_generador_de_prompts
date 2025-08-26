package com.alejandro.microservices.promptgeneratorsaas.dto;

import lombok.Data;

@Data
public class CheckoutSessionRequest {
    private String priceId;
    private String successUrl;
    private String cancelUrl;
}
