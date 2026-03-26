package com.fortalcloud.api.service;

import com.fortalcloud.api.exception.BusinessException;
import com.fortalcloud.api.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;


@Service
public class WasabiService {

    private final S3Client s3Client;

    @Value("${wasabi.bucket}")
    private String bucket;

    @Value("${wasabi.access-key}")
    private String accessKey;

    @Value("${wasabi.secret-key}")
    private String secretKey;

    @Value("${wasabi.endpoint}")
    private String endpoint;

    public WasabiService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public List<String> listBuckets() {
        try {
            return s3Client.listBuckets()
                    .buckets()
                    .stream()
                    .map(b -> b.name())
                    .toList();
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao listar buckets: " + ex.awsErrorDetails().errorMessage());
        }
    }

    public List<String> listFiles(String userId) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(buildBucketName(userId))
                    .build();
            return s3Client.listObjectsV2(request)
                    .contents()
                    .stream()
                    .map(S3Object::key)
                    .toList();
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao listar arquivos do usuário.");
        }
    }

    public URL generateUploadUrl(String userId, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("Nome do arquivo não pode ser vazio.");
        }
        try {
            S3Presigner presigner = buildPresigner();
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .putObjectRequest(r -> r
                            .bucket(buildBucketName(userId))
                            .key( fileName))
                    .build();
            return presigner.presignPutObject(presignRequest).url();
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao gerar URL de upload.");
        }
    }

    public URL generateDownloadUrl(String userId, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("Nome do arquivo não pode ser vazio.");
        }
        try {
            S3Presigner presigner = buildPresigner();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(r -> r
                            .bucket(buildBucketName(userId))
                            .key(userId + "/" + fileName))
                    .build();
            return presigner.presignGetObject(presignRequest).url();
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao gerar URL de download.");
        }
    }

    public void deleteFile(String userId, String fileName) {
        // Verifica se o arquivo existe antes de tentar deletar
        try {
            s3Client.headObject(r -> r
                    .bucket(buildBucketName(userId))
                    .key(fileName));
        } catch (NoSuchKeyException ex) {
            throw new ResourceNotFoundException("Arquivo não encontrado: " + fileName);
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao verificar arquivo antes de deletar.");
        }

        try {
            s3Client.deleteObject(r -> r
                    .bucket(buildBucketName(userId))
                    .key(fileName));
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao deletar arquivo: " + fileName);
        }
    }

    private S3Presigner buildPresigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .build();
    }

    public Map<String, Object> getStorageQuota(String userId){
        try{
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(buildBucketName(userId))
                    .prefix(userId + "/")
                    .build();

            long usedBytes = s3Client.listObjectsV2(request)
                    .contents()
                    .stream()
                    .mapToLong(S3Object::size)
                    .sum();

            long totalBytes = 1_099_511_627_776L; // 1 TB
            double percentUsed = (double) usedBytes / totalBytes * 100;

            return Map.of(
                    "usedBytes", usedBytes,
                    "usedFormatted", formatBytes(usedBytes),
                    "totalBytes", totalBytes,
                    "totalFormatted", "1 TB",
                    "percentUsed", Math.round(percentUsed * 10000.0) / 10000.0
            );
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao calcular quota de armazenamento.");
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public void createUserBucket(String userId) {
        String bucketName = buildBucketName(userId);
        try {
            s3Client.createBucket(r -> r.bucket(bucketName));
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            // bucket já existe, ignora
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao criar bucket para o usuário.");
        }
    }

    private String buildBucketName(String userId) {
        // userId é o email, ex: teste@teste.com → fortal-cloud-teste-teste-com
        return "fortal-cloud-" + userId
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}