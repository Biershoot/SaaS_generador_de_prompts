package com.alejandro.microservices.promptgeneratorsaas.repository;

import com.alejandro.microservices.promptgeneratorsaas.entity.Subscription;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    List<Subscription> findByUser(User user);
    
    Optional<Subscription> findByUserAndStatus(User user, String status);
    
    List<Subscription> findByStatus(String status);
    
    List<Subscription> findByPlan(String plan);
}
