package com.fortalcloud.api.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    public void sendWelcomeEmail(String userEmail) {
        try {
            Resend resend = new Resend(apiKey);
            CreateEmailOptions email = CreateEmailOptions.builder()
                    .from("Fortal-Cloud <noreply@fortal-cloud.com.br>")
                    .to(userEmail)
                    .subject("Bem-vindo ao Fortal-Cloud!")
                    .html("""
                        <h2>Bem-vindo ao Fortal-Cloud!</h2>
                        <p>Sua conta foi criada com sucesso.</p>
                        <p>Acesse sua conta em <a href="https://fortal-cloud.com.br/login">fortal-cloud.com.br</a></p>
                        <br>
                        <p>Equipe Fortal-Cloud</p>
                    """)
                    .build();
            resend.emails().send(email);
        } catch (ResendException e) {
            System.out.println("Erro ao enviar email de boas-vindas: " + e.getMessage());
        }
    }

    public void sendNewUserNotification(String userEmail) {
        try {
            Resend resend = new Resend(apiKey);
            CreateEmailOptions email = CreateEmailOptions.builder()
                    .from("Fortal-Cloud <noreply@fortal-cloud.com.br>")
                    .to("jeffrey@fortal-cloud.com.br")
                    .subject("Novo usuário cadastrado!")
                    .html("""
                        <h2>Novo usuário cadastrado</h2>
                        <p>Um novo usuário se cadastrou na plataforma:</p>
                        <p><strong>Email:</strong> %s</p>
                        <br>
                        <p>Equipe Fortal-Cloud</p>
                    """.formatted(userEmail))
                    .build();
            resend.emails().send(email);
        } catch (ResendException e) {
            System.out.println("Erro ao enviar notificação de novo usuário: " + e.getMessage());
        }
    }
}