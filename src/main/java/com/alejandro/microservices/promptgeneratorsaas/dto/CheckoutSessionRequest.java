package com.alejandro.microservices.promptgeneratorsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CheckoutSessionRequest {
    
    @NotBlank(message = "Price ID is required")
    @Pattern(regexp = "^price_[a-zA-Z0-9_]+$", message = "Invalid price ID format")
    private String priceId;
    
    @NotBlank(message = "Success URL is required")
    @Pattern(regexp = "^https?://[\\w\\d\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$", 
             message = "Invalid success URL format")
    private String successUrl;
    
    @NotBlank(message = "Cancel URL is required")
    @Pattern(regexp = "^https?://[\\w\\d\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$", 
             message = "Invalid cancel URL format")
    private String cancelUrl;
}
