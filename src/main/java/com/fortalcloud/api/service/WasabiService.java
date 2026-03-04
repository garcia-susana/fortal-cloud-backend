package com.fortalcloud.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;

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
        return s3Client.listBuckets()
                .buckets()
                .stream()
                .map(b -> b.name())
                .toList();
    }

    public List<String> listFiles(String userId) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(userId + "/")
                .build();

        return s3Client.listObjectsV2(request)
                .contents()
                .stream()
                .map(S3Object::key)
                .toList();
    }

    public URL generatePresignedUrl(String userId, String fileName) {
        S3Presigner presigner = buildPresigner();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(r -> r
                        .bucket(bucket)
                        .key(userId + "/" + fileName))
                .build();
        return presigner.presignGetObject(presignRequest).url();
    }

    public void deleteFile(String userId, String fileName) {
        s3Client.deleteObject(r -> r
                .bucket(bucket)
                .key(userId + "/" + fileName));
    }
    private S3Presigner buildPresigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .build();
    }
}