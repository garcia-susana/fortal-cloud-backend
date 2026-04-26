package com.fortalcloud.api.controller;

import com.fortalcloud.api.config.JwtService;
import com.fortalcloud.api.exception.BusinessException;
import com.fortalcloud.api.model.User;
import com.fortalcloud.api.repository.UserRepository;
import com.fortalcloud.api.service.EmailService;
import com.fortalcloud.api.service.WasabiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final WasabiService wasabiService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService, WasabiService wasabiService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.wasabiService = wasabiService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String name = body.get("name");
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email ja cadastrado.");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
        wasabiService.createUserBucket(email);
        Map<String, String> iamCredentials = wasabiService.createIamUser(email);
        user.setWasabiUsername(iamCredentials.get("wasabiUsername"));
        user.setWasabiAccessKey(iamCredentials.get("wasabiAccessKey"));
        user.setWasabiSecretKey(iamCredentials.get("wasabiSecretKey"));
        userRepository.save(user);
        emailService.sendWelcomeEmail(email);
        emailService.sendNewUserNotification(email);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(Map.of("token", token, "name", user.getName(), "wasabiAccessKey", user.getWasabiAccessKey()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("Email ou senha invalidos.");
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Email ou senha invalidos.");
        }
        String token = jwtService.generateToken(user);

        Map<String, Object> response = new HashMap<>();

        response.put("token", token);
        response.put("name", user.getName() != null ? user.getName() : "Usuário");
        response.put("wasabiAccessKey", user.getWasabiAccessKey() != null ? user.getWasabiAccessKey() : "");

        return ResponseEntity.ok(response);
    }
}