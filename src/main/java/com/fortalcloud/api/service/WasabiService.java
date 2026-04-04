package com.fortalcloud.api.service;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(WasabiService.class);

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024 * 1024; // 5TB

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

    public Map<String, Object> listFiles(String userId, String path) {
        try {
            String prefix = (path == null || path.isBlank()) ? "" : path.endsWith("/") ? path : path + "/";

            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(buildBucketName(userId))
                    .prefix(prefix)
                    .delimiter("/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            List<String> folders = response.commonPrefixes().stream()
                    .map(CommonPrefix::prefix)
                    .toList();

            List<Map<String, Object>> files = response.contents().stream()
                    .filter(obj -> !obj.key().equals(prefix))
                    .map(obj -> Map.<String, Object>of(
                            "key", obj.key(),
                            "name", obj.key().substring(prefix.length()),
                            "size", obj.size(),
                            "sizeFormatted", formatBytes(obj.size()),
                            "lastModified", obj.lastModified().toString()))
                    .toList();

            return Map.of("folders", folders, "files", files, "currentPath", prefix);
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao listar arquivos do usuário.");
        }
    }

    public URL generateUploadUrl(String userId, String fileName, String path) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("Nome do arquivo não pode ser vazio.");
        }
        try {
            String prefix = (path == null || path.isBlank()) ? "" : path.endsWith("/") ? path : path + "/";
            String key = prefix + fileName;

            S3Presigner presigner = buildPresigner();
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .putObjectRequest(r -> r
                            .bucket(buildBucketName(userId))
                            .key(key))
                    .build();
            return presigner.presignPutObject(presignRequest).url();
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao gerar URL de upload.");
        }
    }

    public String generateShareUrl(String userId, String fileName, int expiryHours) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("Nome do arquivo não pode ser vazio.");
        }
        try {
            S3Presigner presigner = buildPresigner();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(expiryHours))
                    .getObjectRequest(r -> r
                            .bucket(buildBucketName(userId))
                            .key(fileName))
                    .build();
            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao gerar link de compartilhamento.");
        }
    }

    public void createFolder(String userId, String folderPath) {
        if (folderPath == null || folderPath.isBlank()) {
            throw new BusinessException("Nome da pasta não pode ser vazio.");
        }
        String key = folderPath.endsWith("/") ? folderPath : folderPath + "/";
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(buildBucketName(userId))
                            .key(key)
                            .contentLength(0L)
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.empty());
            log.info("Pasta criada: {}/{}", userId, key);
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao criar pasta.");
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
                            .key(fileName)
                            .responseContentDisposition("attachment; filename=\"" + fileName + "\""))
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

    public void deleteBucket(String email) {
        String bucketName = buildBucketName(email);

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Response listResponse;
        do {
            listResponse = s3Client.listObjectsV2(listRequest);
            List<ObjectIdentifier> objetos = listResponse.contents().stream()
                    .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                    .toList();

            if (!objetos.isEmpty()) {
                DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder().objects(objetos).build())
                        .build();
                s3Client.deleteObjects(deleteRequest);
            }

            listRequest = listRequest.toBuilder()
                    .continuationToken(listResponse.nextContinuationToken())
                    .build();
        } while (listResponse.isTruncated());

        // 2. Deletar o bucket
        s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
        log.info("Bucket deletado: {}", bucketName);
    }

    private S3Presigner buildPresigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .build();
    }

    public Map<String, Object> getStorageQuota(String userId) {
        try {
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
                    "percentUsed", Math.round(percentUsed * 10000.0) / 10000.0);
        } catch (S3Exception ex) {
            throw new BusinessException("Erro ao calcular quota de armazenamento.");
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
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

    public Map<String, String> createIamUser(String email) {
        String username = "fc-" + email
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        String bucketName = buildBucketName(email);

        IamClient iam = IamClient.builder()
                .endpointOverride(URI.create("https://iam.wasabisys.com"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .build();

        try {
            iam.createUser(r -> r.userName(username));
        } catch (EntityAlreadyExistsException e) {
            log.warn("IAM user já existe: {}", username);
        }

        String policyDocument = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Action": ["s3:*"],
                            "Resource": [
                                "arn:aws:s3:::%s",
                                "arn:aws:s3:::%s/*"
                            ]
                        }
                    ]
                }
                """.formatted(bucketName, bucketName);

        String policyName = "fc-policy-" + username;
        try {
            CreatePolicyResponse policyResponse = iam.createPolicy(r -> r
                    .policyName(policyName)
                    .policyDocument(policyDocument));

            iam.attachUserPolicy(r -> r
                    .userName(username)
                    .policyArn(policyResponse.policy().arn()));
        } catch (EntityAlreadyExistsException e) {
            log.warn("Policy já existe: {}", policyName);
        }

        CreateAccessKeyResponse keyResponse = iam.createAccessKey(r -> r.userName(username));

        log.info("IAM user criado para: {}", email);

        return Map.of(
                "wasabiUsername", username,
                "wasabiAccessKey", keyResponse.accessKey().accessKeyId(),
                "wasabiSecretKey", keyResponse.accessKey().secretAccessKey());
    }

    private String buildBucketName(String userId) {
        // userId é o email, ex: teste@teste.com → fortal-cloud-teste-teste-com
        return "fortal-cloud-" + userId
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    public void validateFileSize(long fileSize) {
        if (fileSize > MAX_FILE_SIZE) {
            throw new BusinessException("Arquivo excede o limite máximo de 5TB.");
        }
    }
}