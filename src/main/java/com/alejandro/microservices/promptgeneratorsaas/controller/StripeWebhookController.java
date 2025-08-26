package com.alejandro.microservices.promptgeneratorsaas.controller;

import com.alejandro.microservices.promptgeneratorsaas.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook-secret}")
    private String endpointSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.error("Error reading webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error reading webhook payload");
        }

        log.info("Received Stripe event: {}", event.getType());

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "customer.subscription.created":
                    handleSubscriptionCreated(event);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                default:
                    log.info("Unhandled event type: {}", event.getType());
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error processing webhook");
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) event.getData().getObject();
        log.info("Checkout session completed: {}", session.getId());
        
        // The subscription will be handled by the subscription.created event
    }

    private void handleSubscriptionCreated(Event event) {
        Subscription subscription = (Subscription) event.getData().getObject();
        log.info("Subscription created: {}", subscription.getId());
        
        paymentService.handleSubscriptionCreated(
                subscription.getId(),
                subscription.getCustomer(),
                subscription.getItems().getData().get(0).getPrice().getId()
        );
    }

    private void handleSubscriptionUpdated(Event event) {
        Subscription subscription = (Subscription) event.getData().getObject();
        log.info("Subscription updated: {}", subscription.getId());
        
        paymentService.handleSubscriptionUpdated(
                subscription.getId(),
                subscription.getStatus()
        );
    }

    private void handleSubscriptionDeleted(Event event) {
        Subscription subscription = (Subscription) event.getData().getObject();
        log.info("Subscription deleted: {}", subscription.getId());
        
        paymentService.handleSubscriptionDeleted(subscription.getId());
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) event.getData().getObject();
        log.info("Invoice payment succeeded: {}", invoice.getId());
        
        // Update subscription status to active if needed
        if (invoice.getSubscription() != null) {
            paymentService.handleSubscriptionUpdated(
                    invoice.getSubscription(),
                    "active"
            );
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) event.getData().getObject();
        log.info("Invoice payment failed: {}", invoice.getId());
        
        // Update subscription status to past_due
        if (invoice.getSubscription() != null) {
            paymentService.handleSubscriptionUpdated(
                    invoice.getSubscription(),
                    "past_due"
            );
        }
    }
}
