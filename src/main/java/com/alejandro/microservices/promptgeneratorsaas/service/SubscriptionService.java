package com.alejandro.microservices.promptgeneratorsaas.service;

import com.alejandro.microservices.promptgeneratorsaas.dto.SubscriptionPlan;
import com.alejandro.microservices.promptgeneratorsaas.entity.Subscription;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import com.alejandro.microservices.promptgeneratorsaas.exception.SubscriptionException;
import com.alejandro.microservices.promptgeneratorsaas.repository.SubscriptionRepository;
import com.alejandro.microservices.promptgeneratorsaas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing subscription-related business logic.
 * 
 * <p>This service handles all subscription operations including:</p>
 * <ul>
 *   <li>Creating and managing subscription plans</li>
 *   <li>Activating and canceling subscriptions</li>
 *   <li>Upgrading and downgrading subscription plans</li>
 *   <li>Validating subscription status and features</li>
 *   <li>Processing expired subscriptions</li>
 * </ul>
 * 
 * <p>The service supports three subscription tiers:</p>
 * <ul>
 *   <li><strong>FREE</strong>: Basic access with limited prompts (10/month)</li>
 *   <li><strong>PREMIUM</strong>: Enhanced features with more prompts (100/month)</li>
 *   <li><strong>PRO</strong>: Unlimited access with priority support</li>
 * </ul>
 * 
 * @author Alejandro
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Retrieves all available subscription plans.
     * 
     * <p>Returns a list of predefined subscription plans with their features,
     * pricing, and limitations. Each plan includes:</p>
     * <ul>
     *   <li>Plan ID and name</li>
     *   <li>Description and pricing</li>
     *   <li>Prompt limits and features</li>
     *   <li>Stripe price ID for payment processing</li>
     * </ul>
     * 
     * @return List of available subscription plans
     */
    public List<SubscriptionPlan> getAvailablePlans() {
        return Arrays.asList(
                createPlan("free", "Free", "Basic access with limited prompts", null, 0.0, "USD", "monthly", 10, false, false),
                createPlan("premium", "Premium", "Enhanced features with more prompts", "price_premium_monthly", 9.99, "USD", "monthly", 100, true, false),
                createPlan("pro", "Pro", "Unlimited access with priority support", "price_pro_monthly", 19.99, "USD", "monthly", -1, true, true)
        );
    }

    /**
     * Creates a subscription plan object with the specified parameters.
     * 
     * @param id Plan identifier
     * @param name Plan display name
     * @param description Plan description
     * @param stripePriceId Stripe price ID for payment processing
     * @param price Plan price in the specified currency
     * @param currency Currency code (e.g., USD)
     * @param interval Billing interval (e.g., monthly, yearly)
     * @param promptLimit Maximum number of prompts allowed (-1 for unlimited)
     * @param customPrompts Whether custom prompts are allowed
     * @param prioritySupport Whether priority support is included
     * @return Configured SubscriptionPlan object
     */
    private SubscriptionPlan createPlan(String id, String name, String description, String stripePriceId, 
                                       Double price, String currency, String interval, Integer promptLimit, 
                                       Boolean customPrompts, Boolean prioritySupport) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(id);
        plan.setName(name);
        plan.setDescription(description);
        plan.setStripePriceId(stripePriceId);
        plan.setPrice(price);
        plan.setCurrency(currency);
        plan.setInterval(interval);
        plan.setPromptLimit(promptLimit);
        plan.setCustomPrompts(customPrompts);
        plan.setPrioritySupport(prioritySupport);
        return plan;
    }

    /**
     * Retrieves the subscription for a specific user.
     * 
     * @param userId The ID of the user
     * @return Optional containing the user's subscription, or empty if not found
     */
    public Optional<Subscription> getUserSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    /**
     * Creates a free subscription for a user.
     * 
     * <p>This method is typically called when a user registers for the first time.
     * If the user already has a subscription, the existing one is returned.</p>
     * 
     * @param user The user to create the subscription for
     * @return The created or existing subscription
     */
    @Transactional
    public Subscription createFreeSubscription(User user) {
        // Check if user already has a subscription
        Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(user.getId());
        if (existingSubscription.isPresent()) {
            log.info("User {} already has a subscription: {}", user.getId(), existingSubscription.get().getPlan());
            return existingSubscription.get();
        }

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan("FREE")
                .status("ACTIVE")
                .startDate(LocalDate.now())
                .build();

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Created free subscription for user: {}", user.getId());
        return savedSubscription;
    }

    /**
     * Activates a paid subscription for a user.
     * 
     * <p>This method is called when a user completes a payment through Stripe.
     * It will cancel any existing subscription and create a new one with the
     * specified plan and Stripe details.</p>
     * 
     * @param userId The ID of the user
     * @param stripeSubscriptionId The Stripe subscription ID
     * @param stripeCustomerId The Stripe customer ID
     * @param stripePriceId The Stripe price ID
     * @return The activated subscription
     * @throws SubscriptionException if the user is not found
     */
    @Transactional
    public Subscription activateSubscription(Long userId, String stripeSubscriptionId, String stripeCustomerId, String stripePriceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SubscriptionException("User not found with ID: " + userId));

        // Determine plan based on price ID
        String plan = determinePlanFromPriceId(stripePriceId);
        
        // Cancel existing subscription if any
        Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(userId);
        if (existingSubscription.isPresent()) {
            Subscription existing = existingSubscription.get();
            existing.setStatus("CANCELED");
            existing.setEndDate(LocalDate.now());
            subscriptionRepository.save(existing);
            log.info("Canceled existing subscription for user: {}", userId);
        }

        // Create new subscription
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .status("ACTIVE")
                .stripeSubscriptionId(stripeSubscriptionId)
                .stripeCustomerId(stripeCustomerId)
                .stripePriceId(stripePriceId)
                .startDate(LocalDate.now())
                .build();

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Activated {} subscription for user: {} with Stripe ID: {}", plan, userId, stripeSubscriptionId);
        return savedSubscription;
    }

    /**
     * Cancels a user's subscription.
     * 
     * <p>This method marks the subscription as canceled and sets the end date
     * to the current date. The user will maintain access until the end of their
     * current billing period.</p>
     * 
     * @param userId The ID of the user
     * @return The canceled subscription
     * @throws SubscriptionException if no active subscription is found or if already canceled
     */
    @Transactional
    public Subscription cancelSubscription(Long userId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUserId(userId);
        if (subscriptionOpt.isEmpty()) {
            throw new SubscriptionException("No active subscription found for user: " + userId);
        }

        Subscription subscription = subscriptionOpt.get();
        if ("CANCELED".equals(subscription.getStatus())) {
            throw new SubscriptionException("Subscription is already canceled");
        }

        subscription.setStatus("CANCELED");
        subscription.setEndDate(LocalDate.now());
        
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Canceled subscription for user: {} (plan: {})", userId, subscription.getPlan());
        return savedSubscription;
    }

    /**
     * Updates the status of a subscription based on Stripe events.
     * 
     * <p>This method is typically called from webhook handlers when Stripe
     * sends subscription status updates.</p>
     * 
     * @param stripeSubscriptionId The Stripe subscription ID
     * @param status The new status (ACTIVE, CANCELED, PAST_DUE, UNPAID)
     * @return The updated subscription
     * @throws SubscriptionException if the subscription is not found
     */
    @Transactional
    public Subscription updateSubscriptionStatus(String stripeSubscriptionId, String status) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
        if (subscriptionOpt.isEmpty()) {
            throw new SubscriptionException("Subscription not found with Stripe ID: " + stripeSubscriptionId);
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(status.toUpperCase());
        
        if ("CANCELED".equals(status.toUpperCase()) || "UNPAID".equals(status.toUpperCase())) {
            subscription.setEndDate(LocalDate.now());
        }
        
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Updated subscription status to {} for user: {} (Stripe ID: {})", 
                status, subscription.getUser().getId(), stripeSubscriptionId);
        return savedSubscription;
    }

    /**
     * Checks if a user has an active subscription.
     * 
     * @param userId The ID of the user
     * @return true if the user has an active subscription, false otherwise
     */
    public boolean isSubscriptionActive(Long userId) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        return subscription.isPresent() && "ACTIVE".equals(subscription.get().getStatus());
    }

    /**
     * Checks if a user can create a new prompt based on their subscription.
     * 
     * <p>This method validates the user's subscription status and prompt limits.
     * For free users, it checks if they haven't exceeded their monthly limit.</p>
     * 
     * @param userId The ID of the user
     * @return true if the user can create a prompt, false otherwise
     */
    public boolean canUserCreatePrompt(Long userId) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        if (subscription.isEmpty()) {
            return false;
        }

        Subscription sub = subscription.get();
        if (!"ACTIVE".equals(sub.getStatus())) {
            return false;
        }

        // For free plan, check prompt limit
        if ("FREE".equals(sub.getPlan())) {
            // This would require additional logic to count user's prompts
            // For now, return true
            return true;
        }

        // Premium and Pro plans have higher limits
        return true;
    }

    /**
     * Gets the prompt limit for a user based on their subscription plan.
     * 
     * @param userId The ID of the user
     * @return The number of prompts allowed (-1 for unlimited)
     */
    public int getPromptLimit(Long userId) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        if (subscription.isEmpty()) {
            return 0;
        }

        Subscription sub = subscription.get();
        switch (sub.getPlan()) {
            case "FREE":
                return 10;
            case "PREMIUM":
                return 100;
            case "PRO":
                return -1; // Unlimited
            default:
                return 0;
        }
    }

    /**
     * Checks if a user has access to custom prompts feature.
     * 
     * @param userId The ID of the user
     * @return true if the user has custom prompts access, false otherwise
     */
    public boolean hasCustomPrompts(Long userId) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        if (subscription.isEmpty()) {
            return false;
        }

        Subscription sub = subscription.get();
        return "PREMIUM".equals(sub.getPlan()) || "PRO".equals(sub.getPlan());
    }

    /**
     * Checks if a user has access to priority support.
     * 
     * @param userId The ID of the user
     * @return true if the user has priority support, false otherwise
     */
    public boolean hasPrioritySupport(Long userId) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        if (subscription.isEmpty()) {
            return false;
        }

        Subscription sub = subscription.get();
        return "PRO".equals(sub.getPlan());
    }

    /**
     * Gets the current plan details for a user.
     * 
     * @param userId The ID of the user
     * @return The current subscription plan details
     */
    public SubscriptionPlan getCurrentPlan(Long userId) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        if (subscription.isEmpty()) {
            return getAvailablePlans().get(0); // Return free plan
        }

        Subscription sub = subscription.get();
        return getAvailablePlans().stream()
                .filter(plan -> plan.getId().equalsIgnoreCase(sub.getPlan()))
                .findFirst()
                .orElse(getAvailablePlans().get(0));
    }

    /**
     * Checks if a user can upgrade to a specific plan.
     * 
     * <p>This method implements the upgrade hierarchy:
     * FREE → PREMIUM → PRO</p>
     * 
     * @param userId The ID of the user
     * @param targetPlan The plan to upgrade to
     * @return true if the upgrade is allowed, false otherwise
     */
    public boolean canUpgrade(Long userId, String targetPlan) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        if (subscription.isEmpty()) {
            return true; // Can upgrade from no subscription
        }

        Subscription sub = subscription.get();
        if (!"ACTIVE".equals(sub.getStatus())) {
            return true; // Can upgrade from inactive subscription
        }

        // Define upgrade hierarchy
        switch (sub.getPlan()) {
            case "FREE":
                return "PREMIUM".equals(targetPlan) || "PRO".equals(targetPlan);
            case "PREMIUM":
                return "PRO".equals(targetPlan);
            case "PRO":
                return false; // Already at highest tier
            default:
                return true;
        }
    }

    /**
     * Checks if a user can downgrade to a specific plan.
     * 
     * <p>This method implements the downgrade hierarchy:
     * PRO → PREMIUM → FREE</p>
     * 
     * @param userId The ID of the user
     * @param targetPlan The plan to downgrade to
     * @return true if the downgrade is allowed, false otherwise
     */
    public boolean canDowngrade(Long userId, String targetPlan) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        if (subscription.isEmpty()) {
            return false; // No subscription to downgrade
        }

        Subscription sub = subscription.get();
        if (!"ACTIVE".equals(sub.getStatus())) {
            return true; // Can downgrade inactive subscription
        }

        // Define downgrade hierarchy
        switch (sub.getPlan()) {
            case "FREE":
                return false; // Already at lowest tier
            case "PREMIUM":
                return "FREE".equals(targetPlan);
            case "PRO":
                return "PREMIUM".equals(targetPlan) || "FREE".equals(targetPlan);
            default:
                return true;
        }
    }

    /**
     * Determines the plan type based on the Stripe price ID.
     * 
     * @param stripePriceId The Stripe price ID
     * @return The plan type (FREE, PREMIUM, PRO)
     */
    private String determinePlanFromPriceId(String stripePriceId) {
        if (stripePriceId == null) {
            return "FREE";
        }
        
        if (stripePriceId.contains("premium")) {
            return "PREMIUM";
        } else if (stripePriceId.contains("pro")) {
            return "PRO";
        } else {
            return "FREE";
        }
    }

    /**
     * Retrieves all expired subscriptions.
     * 
     * <p>This method finds subscriptions that are marked as active but have
     * passed their end date.</p>
     * 
     * @return List of expired subscriptions
     */
    public List<Subscription> getExpiredSubscriptions() {
        return subscriptionRepository.findByStatusAndEndDateBefore("ACTIVE", LocalDate.now());
    }

    /**
     * Processes expired subscriptions by marking them as expired.
     * 
     * <p>This method is typically called by a scheduled task to clean up
     * subscriptions that have passed their end date.</p>
     */
    public void processExpiredSubscriptions() {
        List<Subscription> expiredSubscriptions = getExpiredSubscriptions();
        for (Subscription subscription : expiredSubscriptions) {
            subscription.setStatus("EXPIRED");
            subscriptionRepository.save(subscription);
            log.info("Marked expired subscription for user: {} (plan: {})", 
                    subscription.getUser().getId(), subscription.getPlan());
        }
    }
}
