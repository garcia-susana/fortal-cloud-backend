package com.fortalcloud.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortalcloud.api.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WebhookService {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final UserRepository userRepository;
    private final WasabiService wasabiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebhookService(UserRepository userRepository, WasabiService wasabiService) {
        this.userRepository = userRepository;
        this.wasabiService = wasabiService;
    }

    public void handleEvent(String payload, String sigHeader) throws SignatureVerificationException {
        Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        System.out.println("📩 Evento recebido: " + event.getType());

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.path("data").path("object");

            switch (event.getType()) {

                case "checkout.session.completed" -> {
                    String email = data.path("customer_email").asText(null);
                    String customerId = data.path("customer").asText(null);
                    System.out.println("📧 Email do webhook: " + email);
                    if (email != null) {
                        userRepository.findByEmail(email).ifPresent(user -> {
                            user.setSubscriptionStatus("ACTIVE");
                            user.setPaymentFailedAt(null);
                            if (customerId != null) {
                                user.setStripeCustomerId(customerId);
                            }
                            userRepository.save(user);
                            wasabiService.createUserBucket(email);
                            System.out.println("✅ Assinatura ativada para: " + email);
                        });
                    }
                }

                case "customer.subscription.deleted" -> {
                    String customerId = data.path("customer").asText(null);
                    if (customerId != null) {
                        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
                            user.setSubscriptionStatus("INACTIVE");
                            userRepository.save(user);
                            System.out.println("❌ Assinatura cancelada para: " + user.getEmail());
                        });
                    }
                }

                case "invoice.payment_failed" -> {
                    String customerId = data.path("customer").asText(null);
                    if (customerId != null) {
                        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
                            if (user.getPaymentFailedAt() == null) {
                                user.setPaymentFailedAt(LocalDateTime.now());
                                userRepository.save(user);
                                System.out.println("⚠️ Inadimplência registrada para: " + user.getEmail());
                            }
                        });
                    }
                }

                case "invoice.paid", "invoice.payment_succeeded" -> {
                    String customerId = data.path("customer").asText(null);
                    if (customerId != null) {
                        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
                            user.setPaymentFailedAt(null);
                            user.setSubscriptionStatus("ACTIVE");
                            userRepository.save(user);
                            wasabiService.createUserBucket(user.getEmail());
                            System.out.println("✅ Pagamento regularizado para: " + user.getEmail());
                        });
                    }
                }

                default -> System.out.println("⏭️ Evento ignorado: " + event.getType());
            }

        } catch (Exception e) {
            System.out.println("❌ Erro ao processar webhook: " + e.getMessage());
        }
    }
}