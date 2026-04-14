package com.fortalcloud.api.controller;

import com.fortalcloud.api.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final EmailService emailService;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<?> contact(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        String message = body.get("message");

        if (name == null || email == null || message == null ||
                name.isBlank() || email.isBlank() || message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Todos os campos são obrigatórios."));
        }

        emailService.sendContactEmail(name, email, message);
        return ResponseEntity.ok(Map.of("message", "Mensagem enviada com sucesso!"));
    }
}