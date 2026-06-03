package com.fortalcloud.api.controller;

import com.fortalcloud.api.exception.BusinessException;
import com.fortalcloud.api.exception.SubscriptionRequiredException;
import com.fortalcloud.api.repository.UserRepository;
import com.fortalcloud.api.service.WasabiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wasabi")
public class WasabiController {

    private final WasabiService wasabiService;
    private final UserRepository userRepository;

    public WasabiController(WasabiService wasabiService, UserRepository userRepository) {
        this.wasabiService = wasabiService;
        this.userRepository = userRepository;
    }

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
                        "bucketName",
                        "fortal-cloud-" + email.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-")
                                .replaceAll("^-|-$", ""),
                        "endpoint", "s3.wasabisys.com",
                        "region", "us-east-1")))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
    }

    @GetMapping("/share-url/{fileName}")
    public ResponseEntity<Map<String, String>> getShareUrl(
            @PathVariable String fileName,
            @RequestParam(required = false, defaultValue = "24") int expiry,
            Authentication authentication) {
        String userId = authentication.getName();
        checkSubscription(userId);
        String url = wasabiService.generateShareUrl(userId, fileName, expiry);
        return ResponseEntity.ok(Map.of(
                "url", url,
                "expiresIn", expiry + "h"));
    }

    @PostMapping("/setup")
    public ResponseEntity<Void> setupUserStorage(Authentication authentication) {
        String userId = authentication.getName();
        userRepository.findByEmail(userId).ifPresent(user -> {
            wasabiService.createUserBucket(userId);
            if (user.getWasabiUsername() == null || user.getWasabiUsername().isBlank()) {
                Map<String, String> iamCredentials = wasabiService.createIamUser(userId);
                user.setWasabiUsername(iamCredentials.get("wasabiUsername"));
                user.setWasabiAccessKey(iamCredentials.get("wasabiAccessKey"));
                user.setWasabiSecretKey(iamCredentials.get("wasabiSecretKey"));
                userRepository.save(user);
            }
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/usage-history")
    public ResponseEntity<List<Map<String, Object>>> getUsageHistory(Authentication authentication){
        String userId = authentication.getName();
        checkSubscription(userId);

        List<Map<String, Object>> history = new ArrayList<>();

        LocalDate today = LocalDate.now();
        for(int i = 6; i >= 0; i--){
            LocalDate date = today.minusDays(i);
            long usedBytes = (long) (Math.random() * 1_073_741_824);

            history.add(Map.of(
                    "date", date.toString(),
                    "usedBytes", usedBytes,
                    "usedGb", Math.round((double) usedBytes / (1024 * 1024 * 1024) * 100.0) / 100.0
            ));
        }
        return ResponseEntity.ok(history);
    }
}