package com.fortalcloud.api.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortalcloud.api.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class WebhookService {
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public WebhookService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public void handleEvent(String payload, String sigHeader) throws SignatureVerificationException {
        Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        System.out.println("📩 Evento recebido: " + event.getType());
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.path("data").path("object");
            if ("checkout.session.completed".equals(event.getType())) {
                String email = data.path("customer_email").asText(null);
                String customerId = data.path("customer").asText(null);
                System.out.println("📧 Email do webhook: " + email);
                if (email != null) {
                    userRepository.findByEmail(email).ifPresent(user -> {
                        user.setSubscriptionStatus("ACTIVE");
                        if(customerId != null){
                            user.setStripeCustomerId(customerId);
                        }
                        userRepository.save(user);
                        System.out.println("✅ Assinatura ativada para: " + email);
                    });
                }
            } else if ("customer.subscription.deleted".equals(event.getType())) {
                String customerId = data.path("customer").asText(null);
                if (customerId != null) {
                    userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
                                user.setSubscriptionStatus("INACTIVE");
                                userRepository.save(user);
                                System.out.println("❌ Assinatura cancelada para: " + user.getEmail());
                            });
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erro ao processar webhook: " + e.getMessage());
        }
    }
}
