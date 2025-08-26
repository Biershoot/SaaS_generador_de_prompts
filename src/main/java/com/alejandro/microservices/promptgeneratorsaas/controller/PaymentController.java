package com.alejandro.microservices.promptgeneratorsaas.controller;

import com.alejandro.microservices.promptgeneratorsaas.dto.CheckoutSessionRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.CheckoutSessionResponse;
import com.alejandro.microservices.promptgeneratorsaas.dto.SubscriptionPlan;
import com.alejandro.microservices.promptgeneratorsaas.service.PaymentService;
import com.alejandro.microservices.promptgeneratorsaas.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/create-checkout-session")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            Authentication authentication,
            @Valid @RequestBody CheckoutSessionRequest request) {
        
        try {
            // Extract user ID from authentication
            Long userId = Long.parseLong(authentication.getName());
            
            log.info("Creating checkout session for user: {} with price ID: {}", 
                    userId, request.getPriceId());
            
            CheckoutSessionResponse response = paymentService.createCheckoutSession(userId, request);
            
            log.info("Checkout session created successfully for user: {} with session ID: {}", 
                    userId, response.getSessionId());
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format in authentication: {}", authentication.getName());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating checkout session for user: {}", 
                    authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<SubscriptionPlan>> getAvailablePlans() {
        try {
            log.info("Retrieving available subscription plans");
            List<SubscriptionPlan> plans = subscriptionService.getAvailablePlans();
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("Error retrieving subscription plans", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
