package com.deeptruth.deeptruth.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.deeptruth.deeptruth.base.exception.FileEmptyException;
import com.deeptruth.deeptruth.base.exception.InvalidFilenameException;
import com.deeptruth.deeptruth.base.exception.S3UploadFailedException;
import com.deeptruth.deeptruth.base.exception.UnsupportedMediaTypeException;
import com.deeptruth.deeptruth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AmazonS3Service {

    @Value("${spring.s3.bucket}")
    private String bucketName;

    private final AmazonS3Client amazonS3Client;

    private final UserRepository userRepository;

    private static final Set<String> ALLOWED_PREFIXES = Set.of("image/", "video/", "application/octet-stream");

    private void validateMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new FileEmptyException();

        String contentType = file.getContentType();
        if (contentType == null || ALLOWED_PREFIXES.stream().noneMatch(contentType::startsWith)) {
            throw new UnsupportedMediaTypeException(contentType);
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank() || !original.contains(".")) {
            throw new InvalidFilenameException(String.valueOf(original));
        }
    }

    private String buildKey(String folder, String originalFilename) {
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            throw new InvalidFilenameException(originalFilename);
        }
        String ext = originalFilename.substring(dot + 1);
        return folder + "/" + UUID.randomUUID() + "." + ext;
    }

    public String uploadFile(String folder, MultipartFile multipartFile) {
        validateMultipart(multipartFile);

        String uploadFileUrl = "";
        String keyName = null;

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(multipartFile.getSize());
        objectMetadata.setContentType(multipartFile.getContentType());

        try (InputStream inputStream = multipartFile.getInputStream()) {

            keyName = buildKey(folder, multipartFile.getOriginalFilename());

            // S3에 폴더 및 파일 업로드
            amazonS3Client.putObject(
                    new PutObjectRequest(bucketName, keyName, inputStream, objectMetadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead));

            // S3에 업로드한 폴더 및 파일 URL
            uploadFileUrl = amazonS3Client.getUrl(bucketName, keyName).toString();

        } catch (IOException | RuntimeException e) {
            log.error("S3 업로드 실패 (key: {})", keyName, e);
            throw new S3UploadFailedException(keyName, e);
        }

        return uploadFileUrl;
    }

    public String uploadBase64Image(InputStream inputStream, String key) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/jpeg");
            metadata.setContentLength(inputStream.available());

            amazonS3Client.putObject(
                    new PutObjectRequest(bucketName, key, inputStream, metadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead)
            );

            return amazonS3Client.getUrl(bucketName, key).toString();
        } catch (IOException | RuntimeException e) {
            log.error("S3 이미지 업로드 실패 (key: {})", key, e);
            throw new S3UploadFailedException(key, e);
        }
    }

    public String uploadStream(InputStream inputStream, String key, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);

            // 1) ByteArrayInputStream이면 available()로 정확한 길이 설정 가능
            if (inputStream instanceof java.io.ByteArrayInputStream) {
                int len = inputStream.available();
                metadata.setContentLength(len);

                amazonS3Client.putObject(
                        new PutObjectRequest(bucketName, key, inputStream, metadata)
                                .withCannedAcl(CannedAccessControlList.PublicRead)
                );

            } else {
                // 2) 길이를 모르면 메모리에 한 번 버퍼링해서 Content-Length 설정
                byte[] bytes = inputStream.readAllBytes(); // Java 11+
                metadata.setContentLength(bytes.length);

                try (InputStream bais = new java.io.ByteArrayInputStream(bytes)) {
                    amazonS3Client.putObject(
                            new PutObjectRequest(bucketName, key, bais, metadata)
                                    .withCannedAcl(CannedAccessControlList.PublicRead)
                    );
                }
            }

            return amazonS3Client.getUrl(bucketName, key).toString();

        } catch (IOException | RuntimeException e) {
            log.error("S3 업로드 실패 (key: {})", key, e);
            throw new S3UploadFailedException(key, e);
        }
    }

    public String uploadBinary(InputStream in, String key, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType != null ? contentType : "application/octet-stream");

            amazonS3Client.putObject(new PutObjectRequest(bucketName, key, in, metadata));

            return amazonS3Client.getUrl(bucketName, key).toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload binary to S3", e);
        }
    }

    public InputStream openStream(String key) {
        return amazonS3Client.getObject(bucketName, key).getObjectContent();
    }

}
