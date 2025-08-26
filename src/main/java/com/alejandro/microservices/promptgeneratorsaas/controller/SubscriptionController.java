package com.alejandro.microservices.promptgeneratorsaas.controller;

import com.alejandro.microservices.promptgeneratorsaas.entity.Subscription;
import com.alejandro.microservices.promptgeneratorsaas.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/my-subscription")
    public ResponseEntity<Map<String, Object>> getMySubscription(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Optional<Subscription> subscription = subscriptionService.getUserSubscription(userId);
        
        Map<String, Object> response = new HashMap<>();
        
        if (subscription.isPresent()) {
            Subscription sub = subscription.get();
            response.put("plan", sub.getPlan());
            response.put("status", sub.getStatus());
            response.put("startDate", sub.getStartDate());
            response.put("endDate", sub.getEndDate());
            response.put("promptLimit", subscriptionService.getPromptLimit(userId));
            response.put("hasCustomPrompts", subscriptionService.hasCustomPrompts(userId));
            response.put("hasPrioritySupport", subscriptionService.hasPrioritySupport(userId));
        } else {
            response.put("plan", "FREE");
            response.put("status", "ACTIVE");
            response.put("promptLimit", 10);
            response.put("hasCustomPrompts", false);
            response.put("hasPrioritySupport", false);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/can-create-prompt")
    public ResponseEntity<Map<String, Object>> canCreatePrompt(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        boolean canCreate = subscriptionService.canUserCreatePrompt(userId);
        int promptLimit = subscriptionService.getPromptLimit(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("canCreate", canCreate);
        response.put("promptLimit", promptLimit);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/features")
    public ResponseEntity<Map<String, Object>> getSubscriptionFeatures(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasCustomPrompts", subscriptionService.hasCustomPrompts(userId));
        response.put("hasPrioritySupport", subscriptionService.hasPrioritySupport(userId));
        response.put("promptLimit", subscriptionService.getPromptLimit(userId));
        
        return ResponseEntity.ok(response);
    }
}
