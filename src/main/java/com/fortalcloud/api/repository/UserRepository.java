package com.fortalcloud.api.repository;

import com.fortalcloud.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByStripeCustomerId(String customerId);

    List<User> findByPaymentFailedAtBeforeAndSubscriptionStatus(
            LocalDateTime dateTime, String subscriptionStatus
    );
}
