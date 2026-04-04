package com.fortalcloud.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String stripeCustomerId;

    @Column(nullable = false)
    private String subscriptionStatus = "INACTIVE";

    @Column(name = "payment_failed_at")
    private LocalDateTime paymentFailedAt;

    @Column(name = "wasabi_username")
    private String wasabiUsername;

    @Column(name = "wasabi_access_key")
    private String wasabiAccessKey;

    @Column(name = "wasabi_secret_key")
    private String wasabiSecretKey;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public LocalDateTime getPaymentFailedAt() { return paymentFailedAt; }
    public void setPaymentFailedAt(LocalDateTime paymentFailedAt) { this.paymentFailedAt = paymentFailedAt; }
    public String getWasabiUsername() { return wasabiUsername; }
    public void setWasabiUsername(String wasabiUsername) { this.wasabiUsername = wasabiUsername; }
    public String getWasabiAccessKey() { return wasabiAccessKey; }
    public void setWasabiAccessKey(String wasabiAccessKey) { this.wasabiAccessKey = wasabiAccessKey; }
    public String getWasabiSecretKey() { return wasabiSecretKey; }
    public void setWasabiSecretKey(String wasabiSecretKey) { this.wasabiSecretKey = wasabiSecretKey; }
}