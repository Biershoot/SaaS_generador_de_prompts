package com.alejandro.microservices.promptgeneratorsaas.service;

import com.alejandro.microservices.promptgeneratorsaas.dto.CheckoutSessionRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.CheckoutSessionResponse;
import com.alejandro.microservices.promptgeneratorsaas.entity.Subscription;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import com.alejandro.microservices.promptgeneratorsaas.exception.PaymentException;
import com.alejandro.microservices.promptgeneratorsaas.repository.SubscriptionRepository;
import com.alejandro.microservices.promptgeneratorsaas.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;

import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    public CheckoutSessionResponse createCheckoutSession(Long userId, CheckoutSessionRequest request) {
        try {
            // Validate user exists
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new PaymentException("User not found with ID: " + userId));

            // Validate price ID format
            if (!request.getPriceId().startsWith("price_")) {
                throw new PaymentException("Invalid price ID format");
            }

            // Create or get Stripe customer
            String customerId = getOrCreateStripeCustomer(user);

            // Create checkout session
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(request.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(request.getCancelUrl())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPrice(request.getPriceId())
                                    .build()
                    )
                    .putMetadata("user_id", userId.toString())
                    .build();

            Session session = Session.create(params);

            CheckoutSessionResponse response = new CheckoutSessionResponse();
            response.setSessionId(session.getId());
            response.setSessionUrl(session.getUrl());
            response.setMessage("Checkout session created successfully");

            log.info("Stripe checkout session created: {} for user: {}", session.getId(), userId);
            return response;

        } catch (StripeException e) {
            log.error("Stripe error creating checkout session for user {}: {}", userId, e.getMessage());
            throw new PaymentException("Failed to create checkout session: " + e.getMessage(), e);
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating checkout session for user {}: {}", userId, e.getMessage());
            throw new PaymentException("An unexpected error occurred while creating checkout session", e);
        }
    }

    public CheckoutSessionResponse createUpgradeSession(Long userId, String newPriceId, String successUrl, String cancelUrl) {
        try {
            // Validate user exists
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new PaymentException("User not found with ID: " + userId));

            // Check if user has an active subscription
            Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(userId);
            if (existingSubscription.isEmpty() || !"ACTIVE".equals(existingSubscription.get().getStatus())) {
                throw new PaymentException("No active subscription found for upgrade");
            }

            Subscription currentSubscription = existingSubscription.get();
            if (currentSubscription.getStripeSubscriptionId() == null) {
                throw new PaymentException("Current subscription is not managed by Stripe");
            }

            // Create or get Stripe customer
            String customerId = getOrCreateStripeCustomer(user);

            // Create checkout session for upgrade
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPrice(newPriceId)
                                    .build()
                    )
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("upgrade_from", currentSubscription.getStripeSubscriptionId())
                    .build();

            Session session = Session.create(params);

            CheckoutSessionResponse response = new CheckoutSessionResponse();
            response.setSessionId(session.getId());
            response.setSessionUrl(session.getUrl());
            response.setMessage("Upgrade checkout session created successfully");

            log.info("Stripe upgrade session created: {} for user: {}", session.getId(), userId);
            return response;

        } catch (StripeException e) {
            log.error("Stripe error creating upgrade session for user {}: {}", userId, e.getMessage());
            throw new PaymentException("Failed to create upgrade session: " + e.getMessage(), e);
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating upgrade session for user {}: {}", userId, e.getMessage());
            throw new PaymentException("An unexpected error occurred while creating upgrade session", e);
        }
    }

    private String getOrCreateStripeCustomer(User user) throws StripeException {
        // Check if user already has a Stripe customer ID
        Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(user.getId());
        if (existingSubscription.isPresent() && existingSubscription.get().getStripeCustomerId() != null) {
            log.debug("Using existing Stripe customer: {} for user: {}", 
                    existingSubscription.get().getStripeCustomerId(), user.getId());
            return existingSubscription.get().getStripeCustomerId();
        }

        // Create new Stripe customer
        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getUsername())
                .putMetadata("user_id", user.getId().toString())
                .build();

        Customer customer = Customer.create(customerParams);
        log.info("Created new Stripe customer: {} for user: {}", customer.getId(), user.getId());
        return customer.getId();
    }

    public void handleSubscriptionCreated(String subscriptionId, String customerId, String priceId) {
        try {
            log.info("Handling subscription created: {} for customer: {}", subscriptionId, customerId);
            
            // Find user by customer ID
            Optional<Subscription> existingSubscription = subscriptionRepository.findByStripeCustomerId(customerId);
            if (existingSubscription.isPresent()) {
                // Update existing subscription
                Subscription subscription = existingSubscription.get();
                subscription.setStripeSubscriptionId(subscriptionId);
                subscription.setStripePriceId(priceId);
                subscription.setStatus("ACTIVE");
                subscription.setStartDate(LocalDate.now());
                subscription.setEndDate(null); // Clear end date for active subscription
                subscriptionRepository.save(subscription);
                
                log.info("Subscription activated for user: {}", subscription.getUser().getId());
            } else {
                // Find user by customer ID in Stripe
                Customer customer = Customer.retrieve(customerId);
                String userIdStr = customer.getMetadata().get("user_id");
                if (userIdStr != null) {
                    Long userId = Long.parseLong(userIdStr);
                    subscriptionService.activateSubscription(userId, subscriptionId, customerId, priceId);
                } else {
                    log.warn("No user_id found in customer metadata for customer: {}", customerId);
                }
            }
        } catch (Exception e) {
            log.error("Error handling subscription created for subscription {}: {}", subscriptionId, e.getMessage());
            throw new PaymentException("Failed to handle subscription creation", e);
        }
    }

    public void handleSubscriptionUpdated(String subscriptionId, String status) {
        try {
            log.info("Handling subscription updated: {} with status: {}", subscriptionId, status);
            
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
            if (subscriptionOpt.isPresent()) {
                subscriptionService.updateSubscriptionStatus(subscriptionId, status);
            } else {
                log.warn("No subscription found for subscription ID: {}", subscriptionId);
            }
        } catch (Exception e) {
            log.error("Error handling subscription updated for subscription {}: {}", subscriptionId, e.getMessage());
            throw new PaymentException("Failed to handle subscription update", e);
        }
    }

    public void handleSubscriptionDeleted(String subscriptionId) {
        try {
            log.info("Handling subscription deleted: {}", subscriptionId);
            
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
            if (subscriptionOpt.isPresent()) {
                subscriptionService.updateSubscriptionStatus(subscriptionId, "CANCELED");
            } else {
                log.warn("No subscription found for subscription ID: {}", subscriptionId);
            }
        } catch (Exception e) {
            log.error("Error handling subscription deleted for subscription {}: {}", subscriptionId, e.getMessage());
            throw new PaymentException("Failed to handle subscription deletion", e);
        }
    }

    public void cancelSubscription(Long userId) {
        try {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUserId(userId);
            if (subscriptionOpt.isEmpty()) {
                throw new PaymentException("No active subscription found for user: " + userId);
            }

            Subscription subscription = subscriptionOpt.get();
            if (subscription.getStripeSubscriptionId() == null) {
                // Local subscription, just cancel it
                subscriptionService.cancelSubscription(userId);
                return;
            }

            // Cancel in Stripe
            com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());
            stripeSubscription.cancel();

            // Update local subscription
            subscriptionService.updateSubscriptionStatus(subscription.getStripeSubscriptionId(), "CANCELED");

            log.info("Subscription canceled for user: {} (Stripe ID: {})", userId, subscription.getStripeSubscriptionId());

        } catch (StripeException e) {
            log.error("Stripe error canceling subscription for user {}: {}", userId, e.getMessage());
            throw new PaymentException("Failed to cancel subscription: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error canceling subscription for user {}: {}", userId, e.getMessage());
            throw new PaymentException("Failed to cancel subscription", e);
        }
    }

    public void updateSubscription(Long userId, String newPriceId) {
        try {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUserId(userId);
            if (subscriptionOpt.isEmpty()) {
                throw new PaymentException("No active subscription found for user: " + userId);
            }

            Subscription subscription = subscriptionOpt.get();
            if (subscription.getStripeSubscriptionId() == null) {
                throw new PaymentException("Subscription is not managed by Stripe");
            }

            // Update subscription in Stripe
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(subscription.getStripeSubscriptionId())
                            .setPrice(newPriceId)
                            .build())
                    .build();

            com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());
            stripeSubscription.update(params);

            // Update local subscription
            subscription.setStripePriceId(newPriceId);
            subscriptionRepository.save(subscription);

            log.info("Subscription updated for user: {} with new price: {}", userId, newPriceId);

        } catch (StripeException e) {
            log.error("Stripe error updating subscription for user {}: {}", userId, e.getMessage());
            throw new PaymentException("Failed to update subscription: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating subscription for user {}: {}", userId, e.getMessage());
            throw new PaymentException("Failed to update subscription", e);
        }
    }
}
