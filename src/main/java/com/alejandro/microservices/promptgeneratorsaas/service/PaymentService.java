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
                Subscription subscription = existingSubscription.get();
                subscription.setStripeSubscriptionId(subscriptionId);
                subscription.setStripePriceId(priceId);
                subscription.setStatus("ACTIVE");
                subscription.setStartDate(LocalDate.now());
                subscriptionRepository.save(subscription);
                
                log.info("Subscription activated for user: {}", subscription.getUser().getId());
            } else {
                log.warn("No subscription found for customer ID: {}", customerId);
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
                Subscription subscription = subscriptionOpt.get();
                subscription.setStatus(status.toUpperCase());
                subscriptionRepository.save(subscription);
                
                log.info("Subscription status updated to {} for user: {}", status, subscription.getUser().getId());
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
                Subscription subscription = subscriptionOpt.get();
                subscription.setStatus("CANCELED");
                subscription.setEndDate(LocalDate.now());
                subscriptionRepository.save(subscription);
                
                log.info("Subscription canceled for user: {}", subscription.getUser().getId());
            } else {
                log.warn("No subscription found for subscription ID: {}", subscriptionId);
            }
        } catch (Exception e) {
            log.error("Error handling subscription deleted for subscription {}: {}", subscriptionId, e.getMessage());
            throw new PaymentException("Failed to handle subscription deletion", e);
        }
    }
}
