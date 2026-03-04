package com.fortalcloud.api.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public void handleEvent(String payload, String sigHeader) throws SignatureVerificationException {
        Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                var deserializer = event.getDataObjectDeserializer();
                if (deserializer.getObject().isPresent()) {
                    Session session = (Session) deserializer.getObject().get();
                    System.out.println("✅ Pagamento confirmado para: " + session.getCustomerEmail());
                    // TODO: ativar subscrição do utilizador na base de dados
                } else {
                    System.out.println("✅ checkout.session.completed recebido mas sem objeto deserializado");
                }
            }
            case "customer.subscription.deleted" -> {
                System.out.println("❌ Subscrição cancelada");
                // TODO: bloquear acesso do utilizador
            }
            case "invoice.payment_failed" -> {
                System.out.println("⚠️ Pagamento falhado");
                // TODO: notificar utilizador e iniciar grace period
            }
            default -> System.out.println("Evento não tratado: " + event.getType());
        }
    }
}