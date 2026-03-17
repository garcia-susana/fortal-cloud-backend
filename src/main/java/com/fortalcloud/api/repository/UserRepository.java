package com.fortalcloud.api.repository;

import com.fortalcloud.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByStripeCustomerId(String customerId);
}
