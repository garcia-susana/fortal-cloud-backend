package com.fortalcloud.api.controller;

import com.fortalcloud.api.service.StripeService;
import com.fortalcloud.api.service.WebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    private final StripeService stripeService;
    private final WebhookService webhookService;

    public StripeController(StripeService stripeService, WebhookService webhookService) {
        this.stripeService = stripeService;
        this.webhookService = webhookService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckout(Authentication authentication) {
        try {
            String email = authentication.getName();
            String url = stripeService.createCheckoutSession(email);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (StripeException e) {
            return ResponseEntity.status(500).body("Erro ao criar sessão: " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            webhookService.handleEvent(payload, sigHeader);
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Webhook inválido");
        }
    }
}