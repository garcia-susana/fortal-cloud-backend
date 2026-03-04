package com.fortalcloud.api.controller;

import com.fortalcloud.api.service.WasabiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.List;

@RestController
@RequestMapping("/api/wasabi")
public class WasabiController {

    private final WasabiService wasabiService;

    public WasabiController(WasabiService wasabiService) {
        this.wasabiService = wasabiService;
    }

    @GetMapping("/buckets")
    public ResponseEntity<List<String>> listBuckets() {
        return ResponseEntity.ok(wasabiService.listBuckets());
    }

    @GetMapping("/files/{userId}")
    public ResponseEntity<List<String>> listFiles(@PathVariable String userId) {
        return ResponseEntity.ok(wasabiService.listFiles(userId));
    }

    //traz a URL para download
    @GetMapping("/upload-url/{userId}/{fileName}")
    public ResponseEntity<String> getUploadUrl(
            @PathVariable String userId,
            @PathVariable String fileName) {
        URL url = wasabiService.generatePresignedUrl(userId, fileName);
        return ResponseEntity.ok(url.toString());
    }

    //gera a URL
    @GetMapping("/download-url/{userId}/{fileName}")
    public ResponseEntity<String> getDownloadUrl(
            @PathVariable String userId,
            @PathVariable String fileName) {
        URL url = wasabiService.generatePresignedUrl(userId, fileName);
        return ResponseEntity.ok(url.toString());
    }

    @GetMapping("/files/{userId}/{fileName}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String userId,
            @PathVariable String fileName) {
        wasabiService.deleteFile(userId, fileName);
        return ResponseEntity.noContent().build();
    }
}