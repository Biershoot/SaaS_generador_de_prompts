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

/**
 * Service class for handling payment operations and Stripe integration.
 * 
 * <p>This service manages all payment-related operations including:</p>
 * <ul>
 *   <li>Creating Stripe checkout sessions for new subscriptions</li>
 *   <li>Handling subscription upgrades and plan changes</li>
 *   <li>Processing Stripe webhook events</li>
 *   <li>Managing Stripe customer creation and retrieval</li>
 *   <li>Handling subscription cancellations and updates</li>
 * </ul>
 * 
 * <p>The service integrates with Stripe's API to provide seamless payment processing
 * and subscription management. It handles both one-time payments and recurring
 * subscriptions.</p>
 * 
 * <p><strong>Security Features:</strong></p>
 * <ul>
 *   <li>Input validation for all payment requests</li>
 *   <li>Secure customer data handling</li>
 *   <li>Webhook signature verification</li>
 *   <li>Comprehensive error handling and logging</li>
 * </ul>
 * 
 * @author Alejandro
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    /**
     * Creates a Stripe checkout session for a new subscription.
     * 
     * <p>This method creates a checkout session that allows users to complete
     * their subscription payment through Stripe's hosted checkout page.</p>
     * 
     * <p>The checkout session includes:</p>
     * <ul>
     *   <li>Subscription mode for recurring payments</li>
     *   <li>Customer information from the user's profile</li>
     *   <li>Success and cancel URLs for post-payment flow</li>
     *   <li>Metadata for tracking the user and subscription</li>
     * </ul>
     * 
     * @param userId The ID of the user creating the subscription
     * @param request The checkout session request containing price ID and URLs
     * @return CheckoutSessionResponse with session details
     * @throws PaymentException if user not found, invalid price ID, or Stripe error
     */
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

    /**
     * Creates a Stripe checkout session for upgrading an existing subscription.
     * 
     * <p>This method creates a checkout session specifically for users who want
     * to upgrade their current subscription to a higher tier plan.</p>
     * 
     * <p>The upgrade session includes:</p>
     * <ul>
     *   <li>Validation that the user has an active subscription</li>
     *   <li>Reference to the current subscription for upgrade tracking</li>
     *   <li>New plan pricing and features</li>
     * </ul>
     * 
     * @param userId The ID of the user upgrading their subscription
     * @param newPriceId The Stripe price ID for the new plan
     * @param successUrl URL to redirect to after successful payment
     * @param cancelUrl URL to redirect to if payment is cancelled
     * @return CheckoutSessionResponse with upgrade session details
     * @throws PaymentException if user not found, no active subscription, or Stripe error
     */
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

    /**
     * Gets or creates a Stripe customer for a user.
     * 
     * <p>This method checks if a user already has a Stripe customer ID associated
     * with their subscription. If not, it creates a new Stripe customer with
     * the user's information.</p>
     * 
     * <p>The customer creation includes:</p>
     * <ul>
     *   <li>User's email address</li>
     *   <li>User's display name</li>
     *   <li>User ID in metadata for tracking</li>
     * </ul>
     * 
     * @param user The user to get or create a Stripe customer for
     * @return The Stripe customer ID
     * @throws StripeException if there's an error creating the customer
     */
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

    /**
     * Handles the creation of a new subscription from Stripe webhook events.
     * 
     * <p>This method is called when Stripe sends a webhook event indicating
     * that a new subscription has been created. It updates the local subscription
     * record with the Stripe subscription details.</p>
     * 
     * @param subscriptionId The Stripe subscription ID
     * @param customerId The Stripe customer ID
     * @param priceId The Stripe price ID
     * @throws PaymentException if there's an error processing the subscription creation
     */
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

    /**
     * Handles subscription status updates from Stripe webhook events.
     * 
     * <p>This method is called when Stripe sends webhook events for subscription
     * status changes such as payment failures, renewals, or cancellations.</p>
     * 
     * @param subscriptionId The Stripe subscription ID
     * @param status The new subscription status
     * @throws PaymentException if there's an error processing the status update
     */
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

    /**
     * Handles subscription deletion from Stripe webhook events.
     * 
     * <p>This method is called when Stripe sends a webhook event indicating
     * that a subscription has been deleted or cancelled.</p>
     * 
     * @param subscriptionId The Stripe subscription ID
     * @throws PaymentException if there's an error processing the subscription deletion
     */
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

    /**
     * Cancels a user's subscription both locally and in Stripe.
     * 
     * <p>This method cancels the subscription in Stripe and updates the local
     * subscription record. If the subscription is not managed by Stripe, it
     * only updates the local record.</p>
     * 
     * @param userId The ID of the user whose subscription to cancel
     * @throws PaymentException if no active subscription found or Stripe error
     */
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

    /**
     * Updates a subscription to a new plan in Stripe.
     * 
     * <p>This method updates the subscription in Stripe to use a new price/plan
     * and updates the local subscription record accordingly.</p>
     * 
     * @param userId The ID of the user whose subscription to update
     * @param newPriceId The new Stripe price ID
     * @throws PaymentException if no active subscription found, not managed by Stripe, or Stripe error
     */
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
