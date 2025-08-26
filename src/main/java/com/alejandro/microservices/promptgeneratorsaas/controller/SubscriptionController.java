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

/**
 * REST Controller for managing subscription-related operations.
 * 
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>Retrieving available subscription plans</li>
 *   <li>Managing user subscriptions</li>
 *   <li>Creating checkout sessions for payments</li>
 *   <li>Handling subscription upgrades and cancellations</li>
 *   <li>Validating subscription features and limits</li>
 * </ul>
 * 
 * <p><strong>Security:</strong> All endpoints require authentication and appropriate
 * user roles (USER or ADMIN). Users can only access their own subscription data.</p>
 * 
 * <p><strong>API Base Path:</strong> /api/subscriptions</p>
 * 
 * @author Alejandro
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;

    /**
     * Retrieves all available subscription plans.
     * 
     * <p>This endpoint returns a list of all subscription plans with their
     * features, pricing, and limitations. The response includes:</p>
     * <ul>
     *   <li>Plan ID and display name</li>
     *   <li>Description and pricing information</li>
     *   <li>Prompt limits and feature flags</li>
     *   <li>Stripe price IDs for payment processing</li>
     * </ul>
     * 
     * <p><strong>Access:</strong> Requires USER or ADMIN role</p>
     * 
     * @return ResponseEntity containing list of subscription plans
     */
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

    /**
     * Retrieves the current subscription information for the authenticated user.
     * 
     * <p>This endpoint returns comprehensive information about the user's
     * current subscription including:</p>
     * <ul>
     *   <li>Subscription details (plan, status, dates)</li>
     *   <li>Current plan information</li>
     *   <li>Feature access flags</li>
     *   <li>Prompt limits and usage</li>
     * </ul>
     * 
     * <p><strong>Access:</strong> Requires USER or ADMIN role</p>
     * 
     * @param authentication Spring Security authentication object
     * @return ResponseEntity containing subscription information
     */
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

    /**
     * Creates a Stripe checkout session for a new subscription.
     * 
     * <p>This endpoint creates a checkout session that allows users to complete
     * their subscription payment through Stripe's hosted checkout page.</p>
     * 
     * <p><strong>Request Body:</strong></p>
     * <ul>
     *   <li>priceId: Stripe price ID for the selected plan</li>
     *   <li>successUrl: URL to redirect after successful payment</li>
     *   <li>cancelUrl: URL to redirect if payment is cancelled</li>
     * </ul>
     * 
     * <p><strong>Access:</strong> Requires USER or ADMIN role</p>
     * 
     * @param authentication Spring Security authentication object
     * @param request Checkout session request with payment details
     * @return ResponseEntity containing checkout session information
     */
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

    /**
     * Creates a Stripe checkout session for upgrading an existing subscription.
     * 
     * <p>This endpoint creates a checkout session specifically for users who want
     * to upgrade their current subscription to a higher tier plan.</p>
     * 
     * <p><strong>Query Parameters:</strong></p>
     * <ul>
     *   <li>newPriceId: Stripe price ID for the new plan</li>
     *   <li>successUrl: URL to redirect after successful payment</li>
     *   <li>cancelUrl: URL to redirect if payment is cancelled</li>
     * </ul>
     * 
     * <p><strong>Access:</strong> Requires USER or ADMIN role</p>
     * 
     * @param authentication Spring Security authentication object
     * @param newPriceId The Stripe price ID for the new plan
     * @param successUrl URL to redirect after successful payment
     * @param cancelUrl URL to redirect if payment is cancelled
     * @return ResponseEntity containing upgrade session information
     */
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

    /**
     * Cancels the current subscription for the authenticated user.
     * 
     * <p>This endpoint cancels the user's subscription both locally and in Stripe.
     * The user will maintain access until the end of their current billing period.</p>
     * 
     * <p><strong>Access:</strong> Requires USER or ADMIN role</p>
     * 
     * @param authentication Spring Security authentication object
     * @return ResponseEntity with cancellation confirmation
     */
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

    /**
     * Checks if the user can upgrade to a specific plan.
     * 
     * <p>This endpoint validates whether the user is eligible to upgrade to
     * the specified plan based on their current subscription and upgrade hierarchy.</p>
     * 
     * <p><strong>Query Parameters:</strong></p>
     * <ul>
     *   <li>targetPlan: The plan to upgrade to (free, premium, pro)</li>
     * </ul>
     * 
     * <p><strong>Access:</strong> Requires USER or ADMIN role</p>
     * 
     * @param authentication Spring Security authentication object
     * @param targetPlan The plan to upgrade to
     * @return ResponseEntity with upgrade eligibility information
     */
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

    /**
     * Checks if the user can downgrade to a specific plan.
     * 
     * <p>This endpoint validates whether the user is eligible to downgrade to
     * the specified plan based on their current subscription and downgrade hierarchy.</p>
     * 
     * <p><strong>Query Parameters:</strong></p>
     * <ul>
     *   <li>targetPlan: The plan to downgrade to (free, premium, pro)</li>
     * </ul>
     * 
     * <p><strong>Access:</strong> Requires USER or ADMIN role</p>
     * 
     * @param authentication Spring Security authentication object
     * @param targetPlan The plan to downgrade to
     * @return ResponseEntity with downgrade eligibility information
     */
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

    /**
     * Retrieves the subscription features available to the authenticated user.
     * 
     * <p>This endpoint returns information about the user's current subscription
     * features including prompt limits, custom prompts access, and priority support.</p>
     * 
     * <p><strong>Access:</strong> Requires USER or ADMIN role</p>
     * 
     * @param authentication Spring Security authentication object
     * @return ResponseEntity containing subscription features
     */
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

    /**
     * Updates the user's subscription to a new plan.
     * 
     * <p>This endpoint updates the subscription in Stripe to use a new price/plan
     * and updates the local subscription record accordingly.</p>
     * 
     * <p><strong>Query Parameters:</strong></p>
     * <ul>
     *   <li>newPriceId: The new Stripe price ID</li>
     * </ul>
     * 
     * <p><strong>Access:</strong> Requires USER or ADMIN role</p>
     * 
     * @param authentication Spring Security authentication object
     * @param newPriceId The new Stripe price ID
     * @return ResponseEntity with update confirmation
     */
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
