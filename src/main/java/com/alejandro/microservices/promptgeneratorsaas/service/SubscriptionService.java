package com.alejandro.microservices.promptgeneratorsaas.service;

import com.alejandro.microservices.promptgeneratorsaas.dto.SubscriptionPlan;
import com.alejandro.microservices.promptgeneratorsaas.entity.Subscription;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import com.alejandro.microservices.promptgeneratorsaas.repository.SubscriptionRepository;
import com.alejandro.microservices.promptgeneratorsaas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
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

    public Subscription createFreeSubscription(User user) {
        // Check if user already has a subscription
        Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(user.getId());
        if (existingSubscription.isPresent()) {
            return existingSubscription.get();
        }

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan("FREE")
                .status("ACTIVE")
                .startDate(LocalDate.now())
                .build();

        return subscriptionRepository.save(subscription);
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
}
