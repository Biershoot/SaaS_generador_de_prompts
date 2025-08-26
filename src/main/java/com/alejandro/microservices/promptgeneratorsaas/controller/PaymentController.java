package com.alejandro.microservices.promptgeneratorsaas.controller;

import com.alejandro.microservices.promptgeneratorsaas.dto.CheckoutSessionRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.CheckoutSessionResponse;
import com.alejandro.microservices.promptgeneratorsaas.dto.SubscriptionPlan;
import com.alejandro.microservices.promptgeneratorsaas.service.PaymentService;
import com.alejandro.microservices.promptgeneratorsaas.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            Authentication authentication,
            @RequestBody CheckoutSessionRequest request) {
        
        // Extract user ID from authentication
        Long userId = Long.parseLong(authentication.getName());
        
        CheckoutSessionResponse response = paymentService.createCheckoutSession(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAvailablePlans() {
        List<SubscriptionPlan> plans = subscriptionService.getAvailablePlans();
        return ResponseEntity.ok(plans);
    }
}
