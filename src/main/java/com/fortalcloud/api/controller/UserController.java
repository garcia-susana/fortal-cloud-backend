package com.fortalcloud.api.controller;
import com.fortalcloud.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserRepository userRepository;
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
