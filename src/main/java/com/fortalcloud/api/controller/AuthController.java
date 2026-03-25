package com.fortalcloud.api.controller;

import com.fortalcloud.api.config.JwtService;
import com.fortalcloud.api.exception.BusinessException;
import com.fortalcloud.api.model.User;
import com.fortalcloud.api.repository.UserRepository;
import com.fortalcloud.api.service.EmailService;
import com.fortalcloud.api.service.WasabiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final WasabiService wasabiService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email já cadastrado.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);

        // Envia emails
        wasabiService.createUserBucket(email);
        emailService.sendWelcomeEmail(email);
        emailService.sendNewUserNotification(email);

        String token = jwtService.generateToken(email);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("Email ou senha inválidos.");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Email ou senha inválidos.");
        }

        String token = jwtService.generateToken(email);
        return ResponseEntity.ok(Map.of("token", token));
    }
}