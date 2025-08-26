package com.alejandro.microservices.promptgeneratorsaas.repository;

import com.alejandro.microservices.promptgeneratorsaas.entity.Subscription;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUser(User user);
    Optional<Subscription> findByUserId(Long userId);
    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
