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

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    public List<SubscriptionPlan> getAvailablePlans() {
        return Arrays.asList(
                createPlan("free", "Free", "Basic access with limited prompts", null, 0.0, "USD", "monthly", 10, false, false),
                createPlan("premium", "Premium", "Enhanced features with more prompts", "price_premium_monthly", 9.99, "USD", "monthly", 100, true, false),
                createPlan("pro", "Pro", "Unlimited access with priority support", "price_pro_monthly", 19.99, "USD", "monthly", -1, true, true)
        );
    }

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

    public Optional<Subscription> getUserSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

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

    public boolean isSubscriptionActive(Long userId) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        return subscription.isPresent() && "ACTIVE".equals(subscription.get().getStatus());
    }

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

    public boolean hasCustomPrompts(Long userId) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        if (subscription.isEmpty()) {
            return false;
        }

        Subscription sub = subscription.get();
        return "PREMIUM".equals(sub.getPlan()) || "PRO".equals(sub.getPlan());
    }

    public boolean hasPrioritySupport(Long userId) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        if (subscription.isEmpty()) {
            return false;
        }

        Subscription sub = subscription.get();
        return "PRO".equals(sub.getPlan());
    }

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

    public List<Subscription> getExpiredSubscriptions() {
        return subscriptionRepository.findByStatusAndEndDateBefore("ACTIVE", LocalDate.now());
    }

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
