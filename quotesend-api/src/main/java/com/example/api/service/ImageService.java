package com.example.api.service;

import com.example.api.exception.AppException;
import com.example.api.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class ImageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}") private String bucket;
    @Value("${aws.s3.region}")      private String region;

    private static final long   MAX_SIZE   = 5L * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    public String upload(MultipartFile file) {
        if (file.isEmpty()) throw new AppException(ErrorCode.FILE_EMPTY);
        if (file.getSize() > MAX_SIZE) throw new AppException(ErrorCode.FILE_TOO_LARGE);
        String ct = file.getContentType();
        if (ct == null || !ALLOWED.contains(ct)) throw new AppException(ErrorCode.INVALID_FILE_TYPE);

        String key = "quote-images/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(ct).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
        } catch (IOException e) {
            log.error("S3 upload failed: {}", e.getMessage());
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public void delete(String imageUrl) {
        try {
            // Extract the S3 key from the URL
            URI uri = URI.create(imageUrl);
            String key = uri.getPath().substring(1); // remove leading /
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            log.warn("Failed to delete S3 object: {} - {}", imageUrl, e.getMessage());
        }
    }

    /** Upload raw PDF bytes for email attachment, returns S3 key. */
    public String uploadPdf(byte[] pdfBytes, String filename) {
        String key = "quote-pdfs/" + UUID.randomUUID() + "/" + filename;
        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key)
                            .contentType("application/pdf").build(),
                    RequestBody.fromBytes(pdfBytes));
            return key;
        } catch (Exception e) {
            log.error("S3 PDF upload failed: {}", e.getMessage());
            return null; // non-fatal — email still sends without S3 key
        }
    }
}
