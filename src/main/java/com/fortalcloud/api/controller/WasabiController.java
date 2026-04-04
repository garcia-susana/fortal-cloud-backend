package com.fortalcloud.api.controller;

import com.fortalcloud.api.exception.BusinessException;
import com.fortalcloud.api.exception.SubscriptionRequiredException;
import com.fortalcloud.api.repository.UserRepository;
import com.fortalcloud.api.service.WasabiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wasabi")
@RequiredArgsConstructor
public class WasabiController {

    private final WasabiService wasabiService;
    private final UserRepository userRepository;

    private void checkSubscription(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!"ACTIVE".equals(user.getSubscriptionStatus())) {
                throw new SubscriptionRequiredException();
            }
        });
    }

    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam(required = false, defaultValue = "") String path,
            Authentication authentication) {
        String userId = authentication.getName();
        checkSubscription(userId);
        return ResponseEntity.ok(wasabiService.listFiles(userId, path));
    }

    @GetMapping("/upload-url/{fileName}")
    public ResponseEntity<Map<String, String>> getUploadUrl(
            @PathVariable String fileName,
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestParam(required = false, defaultValue = "0") long fileSize,
            Authentication authentication) {
        String userId = authentication.getName();
        checkSubscription(userId);
        wasabiService.validateFileSize(fileSize);
        String url = wasabiService.generateUploadUrl(userId, fileName, path).toString();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/download-url/{fileName}")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable String fileName,
            Authentication authentication) {
        String userId = authentication.getName();
        checkSubscription(userId);
        String url = wasabiService.generateDownloadUrl(userId, fileName).toString();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping("/files/{fileName}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileName,
            Authentication authentication) {
        String userId = authentication.getName();
        checkSubscription(userId);
        wasabiService.deleteFile(userId, fileName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/folders")
    public ResponseEntity<Void> createFolder(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userId = authentication.getName();
        checkSubscription(userId);
        wasabiService.createFolder(userId, body.get("path"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/quota")
    public ResponseEntity<Map<String, Object>> getQuota(Authentication authentication) {
        String userId = authentication.getName();
        checkSubscription(userId);
        return ResponseEntity.ok(wasabiService.getStorageQuota(userId));
    }

    @GetMapping("/credentials")
    public ResponseEntity<Map<String, String>> getCredentials(Authentication authentication) {
        String email = authentication.getName();
        checkSubscription(email);
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(Map.of(
                        "wasabiUsername", user.getWasabiUsername() != null ? user.getWasabiUsername() : "",
                        "wasabiAccessKey", user.getWasabiAccessKey() != null ? user.getWasabiAccessKey() : "",
                        "wasabiSecretKey", user.getWasabiSecretKey() != null ? user.getWasabiSecretKey() : "",
                        "bucketName", "fortal-cloud-" + email.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("^-|-$", ""),
                        "endpoint", "s3.wasabisys.com",
                        "region", "us-east-1")))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
    }
}