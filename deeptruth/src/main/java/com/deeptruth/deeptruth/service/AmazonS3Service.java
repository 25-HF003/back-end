package com.deeptruth.deeptruth.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.deeptruth.deeptruth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AmazonS3Service {

    @Value("${spring.s3.bucket}")
    private String bucketName;

    private final AmazonS3Client amazonS3Client;

    private final UserRepository userRepository;

    public String uploadFile(String folder, MultipartFile multipartFile) {
        String uploadFileUrl = "";

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(multipartFile.getSize());
        objectMetadata.setContentType(multipartFile.getContentType());

        try (InputStream inputStream = multipartFile.getInputStream()) {

            String keyName = folder + "/" + UUID.randomUUID() + "." + multipartFile.getOriginalFilename();

            // S3에 폴더 및 파일 업로드
            amazonS3Client.putObject(
                    new PutObjectRequest(bucketName, keyName, inputStream, objectMetadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead));

            // S3에 업로드한 폴더 및 파일 URL
            uploadFileUrl = amazonS3Client.getUrl(bucketName, keyName).toString();

        } catch (IOException e) {
            e.printStackTrace();
            log.error("Filed upload failed", e);
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
        } catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            throw new RuntimeException("S3 이미지 업로드 실패", e);
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

        } catch (IOException e) {
            log.error("S3 업로드 실패 (key: {})", key, e);
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }


}
