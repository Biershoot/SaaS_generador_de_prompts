package com.alejandro.microservices.promptgeneratorsaas.service;

import com.alejandro.microservices.promptgeneratorsaas.dto.CheckoutSessionRequest;
import com.alejandro.microservices.promptgeneratorsaas.entity.Subscription;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import com.alejandro.microservices.promptgeneratorsaas.repository.SubscriptionRepository;
import com.alejandro.microservices.promptgeneratorsaas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private CheckoutSessionRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .build();

        testRequest = new CheckoutSessionRequest();
        testRequest.setPriceId("price_test123");
        testRequest.setSuccessUrl("http://localhost:3000/success");
        testRequest.setCancelUrl("http://localhost:3000/cancel");
    }

    @Test
    void testCreateCheckoutSession_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            paymentService.createCheckoutSession(1L, testRequest);
        });
    }

    @Test
    void testHandleSubscriptionCreated() {
        Subscription subscription = Subscription.builder()
                .id(1L)
                .user(testUser)
                .plan("PREMIUM")
                .status("ACTIVE")
                .stripeCustomerId("cus_test123")
                .build();

        when(subscriptionRepository.findByStripeCustomerId("cus_test123"))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(subscription);

        // Should not throw any exception
        assertDoesNotThrow(() -> {
            paymentService.handleSubscriptionCreated("sub_test123", "cus_test123", "price_test123");
        });
    }

    @Test
    void testHandleSubscriptionUpdated() {
        Subscription subscription = Subscription.builder()
                .id(1L)
                .user(testUser)
                .plan("PREMIUM")
                .status("ACTIVE")
                .stripeSubscriptionId("sub_test123")
                .build();

        when(subscriptionRepository.findByStripeSubscriptionId("sub_test123"))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(subscription);

        // Should not throw any exception
        assertDoesNotThrow(() -> {
            paymentService.handleSubscriptionUpdated("sub_test123", "active");
        });
    }

    @Test
    void testHandleSubscriptionDeleted() {
        Subscription subscription = Subscription.builder()
                .id(1L)
                .user(testUser)
                .plan("PREMIUM")
                .status("ACTIVE")
                .stripeSubscriptionId("sub_test123")
                .build();

        when(subscriptionRepository.findByStripeSubscriptionId("sub_test123"))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(subscription);

        // Should not throw any exception
        assertDoesNotThrow(() -> {
            paymentService.handleSubscriptionDeleted("sub_test123");
        });
    }
}
