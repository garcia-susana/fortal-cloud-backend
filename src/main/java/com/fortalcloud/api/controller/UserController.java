package com.fortalcloud.api.controller;

import com.fortalcloud.api.exception.BusinessException;
import com.fortalcloud.api.model.User;
import com.fortalcloud.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(Map.of(
                        "email", user.getEmail(),
                        "subscriptionStatus", user.getSubscriptionStatus()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, Authentication authentication) {
        String email = authentication.getName();
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || newPassword == null ||
                currentPassword.isBlank() || newPassword.isBlank()) {
            throw new BusinessException("Todos os campos sao obrigatorios.");
        }

        if (newPassword.length() < 6) {
            throw new BusinessException("A nova senha deve ter pelo menos 6 caracteres.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario nao encontrado."));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Senha atual incorreta.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
    }
}