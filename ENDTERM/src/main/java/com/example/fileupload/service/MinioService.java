package com.example.fileupload.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket}")
    private String bucketName;
    
    public void uploadFile(String fileName, InputStream inputStream, String contentType, long fileSize) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, fileSize, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("File {} uploaded successfully to bucket {}", fileName, bucketName);
        } catch (Exception e) {
            log.error("Error uploading file {} to MinIO: {}", fileName, e.getMessage());
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }
}

