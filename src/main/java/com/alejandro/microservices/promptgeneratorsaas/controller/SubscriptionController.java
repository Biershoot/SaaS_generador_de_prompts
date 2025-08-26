package com.alejandro.microservices.promptgeneratorsaas.controller;

import com.alejandro.microservices.promptgeneratorsaas.dto.CheckoutSessionRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.CheckoutSessionResponse;
import com.alejandro.microservices.promptgeneratorsaas.dto.SubscriptionPlan;
import com.alejandro.microservices.promptgeneratorsaas.entity.Subscription;
import com.alejandro.microservices.promptgeneratorsaas.service.PaymentService;
import com.alejandro.microservices.promptgeneratorsaas.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;

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

    @GetMapping("/my-subscription")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getMySubscription(Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("Retrieving subscription for user: {}", userId);
            
            Optional<Subscription> subscription = subscriptionService.getUserSubscription(userId);
            SubscriptionPlan currentPlan = subscriptionService.getCurrentPlan(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("subscription", subscription.orElse(null));
            response.put("currentPlan", currentPlan);
            response.put("isActive", subscriptionService.isSubscriptionActive(userId));
            response.put("canCreatePrompt", subscriptionService.canUserCreatePrompt(userId));
            response.put("promptLimit", subscriptionService.getPromptLimit(userId));
            response.put("hasCustomPrompts", subscriptionService.hasCustomPrompts(userId));
            response.put("hasPrioritySupport", subscriptionService.hasPrioritySupport(userId));
            
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format in authentication: {}", authentication.getName());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error retrieving subscription for user: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/create-checkout-session")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            Authentication authentication,
            @Valid @RequestBody CheckoutSessionRequest request) {
        
        try {
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

    @PostMapping("/upgrade")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CheckoutSessionResponse> upgradeSubscription(
            Authentication authentication,
            @RequestParam String newPriceId,
            @RequestParam String successUrl,
            @RequestParam String cancelUrl) {
        
        try {
            Long userId = Long.parseLong(authentication.getName());
            
            log.info("Creating upgrade session for user: {} with new price ID: {}", 
                    userId, newPriceId);
            
            CheckoutSessionResponse response = paymentService.createUpgradeSession(userId, newPriceId, successUrl, cancelUrl);
            
            log.info("Upgrade session created successfully for user: {} with session ID: {}", 
                    userId, response.getSessionId());
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format in authentication: {}", authentication.getName());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating upgrade session for user: {}", 
                    authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> cancelSubscription(Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            
            log.info("Canceling subscription for user: {}", userId);
            
            paymentService.cancelSubscription(userId);
            
            log.info("Subscription canceled successfully for user: {}", userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Subscription canceled successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format in authentication: {}", authentication.getName());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error canceling subscription for user: {}", 
                    authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/can-upgrade")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> canUpgrade(
            Authentication authentication,
            @RequestParam String targetPlan) {
        
        try {
            Long userId = Long.parseLong(authentication.getName());
            
            boolean canUpgrade = subscriptionService.canUpgrade(userId, targetPlan);
            
            Map<String, Object> response = new HashMap<>();
            response.put("canUpgrade", canUpgrade);
            response.put("targetPlan", targetPlan);
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format in authentication: {}", authentication.getName());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error checking upgrade eligibility for user: {}", 
                    authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/can-downgrade")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> canDowngrade(
            Authentication authentication,
            @RequestParam String targetPlan) {
        
        try {
            Long userId = Long.parseLong(authentication.getName());
            
            boolean canDowngrade = subscriptionService.canDowngrade(userId, targetPlan);
            
            Map<String, Object> response = new HashMap<>();
            response.put("canDowngrade", canDowngrade);
            response.put("targetPlan", targetPlan);
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format in authentication: {}", authentication.getName());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error checking downgrade eligibility for user: {}", 
                    authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/features")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getSubscriptionFeatures(Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            
            Map<String, Object> features = new HashMap<>();
            features.put("canCreatePrompt", subscriptionService.canUserCreatePrompt(userId));
            features.put("promptLimit", subscriptionService.getPromptLimit(userId));
            features.put("hasCustomPrompts", subscriptionService.hasCustomPrompts(userId));
            features.put("hasPrioritySupport", subscriptionService.hasPrioritySupport(userId));
            features.put("isActive", subscriptionService.isSubscriptionActive(userId));
            
            return ResponseEntity.ok(features);
            
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format in authentication: {}", authentication.getName());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error retrieving subscription features for user: {}", 
                    authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> updateSubscription(
            Authentication authentication,
            @RequestParam String newPriceId) {
        
        try {
            Long userId = Long.parseLong(authentication.getName());
            
            log.info("Updating subscription for user: {} with new price ID: {}", 
                    userId, newPriceId);
            
            paymentService.updateSubscription(userId, newPriceId);
            
            log.info("Subscription updated successfully for user: {}", userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Subscription updated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format in authentication: {}", authentication.getName());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating subscription for user: {}", 
                    authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
