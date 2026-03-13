package com.fortalcloud.api.controller;

import com.fortalcloud.api.service.WasabiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wasabi")
public class WasabiController {

    private final WasabiService wasabiService;

    public WasabiController(WasabiService wasabiService) {
        this.wasabiService = wasabiService;
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(wasabiService.listFiles(userId));
    }

    @GetMapping("/upload-url/{fileName}")
    public ResponseEntity<Map<String, String>> getUploadUrl(
            @PathVariable String fileName,
            Authentication authentication) {
        String userId = authentication.getName();
        String url = wasabiService.generateUploadUrl(userId, fileName).toString();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/download-url/{fileName}")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable String fileName,
            Authentication authentication) {
        String userId = authentication.getName();
        String url = wasabiService.generateDownloadUrl(userId, fileName).toString();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping("/files/{fileName}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileName,
            Authentication authentication) {
        String userId = authentication.getName();
        wasabiService.deleteFile(userId, fileName);
        return ResponseEntity.noContent().build();
    }
}