package com.jobagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.storage.local.base-path:./uploads}")
    private String localBasePath;

    @Value("${app.storage.s3.bucket:}")
    private String s3Bucket;

    @Value("${app.storage.s3.region:us-east-1}")
    private String s3Region;

    @Value("${app.storage.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${app.storage.s3.access-key:}")
    private String s3AccessKey;

    @Value("${app.storage.s3.secret-key:}")
    private String s3SecretKey;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if ("s3".equals(storageType)) {
            try {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(s3AccessKey, s3SecretKey);
                var builder = S3Client.builder()
                        .region(Region.of(s3Region))
                        .credentialsProvider(StaticCredentialsProvider.create(credentials));
                if (s3Endpoint != null && !s3Endpoint.isBlank()) {
                    builder.endpointOverride(URI.create(s3Endpoint));
                }
                s3Client = builder.build();
                log.info("S3 storage initialized for bucket: {}", s3Bucket);
            } catch (Exception e) {
                log.error("Failed to initialize S3 client: {}. Falling back to local.", e.getMessage());
                storageType = "local";
            }
        }
        if ("local".equals(storageType)) {
            try {
                Files.createDirectories(Path.of(localBasePath));
            } catch (IOException e) {
                log.error("Failed to create local storage directory: {}", e.getMessage());
            }
            log.info("Local storage initialized at: {}", localBasePath);
        }
    }

    public String store(String directory, String fileName, byte[] content, String contentType) {
        String key = directory + "/" + UUID.randomUUID() + "_" + fileName;

        if ("s3".equals(storageType)) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(content));
            log.info("Stored file in S3: {}/{}", s3Bucket, key);
            return "s3://" + s3Bucket + "/" + key;
        }

        try {
            Path path = Path.of(localBasePath, key);
            Files.createDirectories(path.getParent());
            Files.write(path, content);
            log.info("Stored file locally: {}", path);
            return path.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file locally", e);
        }
    }

    public byte[] retrieve(String fileUrl) {
        if (fileUrl.startsWith("s3://")) {
            String[] parts = fileUrl.replace("s3://", "").split("/", 2);
            String bucket = parts[0];
            String key = parts[1];
            return s3Client.getObjectAsBytes(
                    software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()).asByteArray();
        }

        try {
            return Files.readAllBytes(Path.of(fileUrl));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + fileUrl, e);
        }
    }

    public void delete(String fileUrl) {
        if (fileUrl.startsWith("s3://")) {
            String[] parts = fileUrl.replace("s3://", "").split("/", 2);
            s3Client.deleteObject(
                    software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                            .bucket(parts[0])
                            .key(parts[1])
                            .build());
            return;
        }
        try {
            Files.deleteIfExists(Path.of(fileUrl));
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", fileUrl);
        }
    }

    public String getStorageType() {
        return storageType;
    }
}
