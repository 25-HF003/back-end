package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.watermark.InsertResultDTO;
import com.deeptruth.deeptruth.base.dto.watermark.WatermarkFlaskResponseDTO;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.entity.Watermark;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.repository.WatermarkRepository;
import com.deeptruth.deeptruth.util.ImageHashUtils;
import com.deeptruth.deeptruth.util.ImageNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class WatermarkService {
    private final WatermarkRepository watermarkRepository;
    private final UserRepository userRepository;
    private final AmazonS3Service amazonS3Service;
    private final WebClient webClient;
    private final ActiveTaskService activeTaskService;

    @Value("${flask.watermarkServer.url}")
    private String flaskServerUrl;

    public InsertResultDTO insert(Long userId, MultipartFile file, String message, String taskId) {
        // 1) 유효성
        if (message == null | message.isBlank()) {
            throw new IllegalArgumentException("message는 null일 수 없습니다.");
        }
        if (message.length() > 4) {
            throw new IllegalArgumentException("message는 최대 4자입니다.");
        }
        if (taskId == null || taskId.isBlank()) {
            taskId = java.util.UUID.randomUUID().toString();
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (file == null || file.isEmpty()) throw new FileEmptyException();
        String contentType = (file.getContentType() != null) ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        // image/* 만 허용
        if (!(contentType.startsWith("image/") || MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType))) {
            throw new UnsupportedMediaTypeException(contentType);
        }

        // 2) 원본 바이트 & 해시 계산
        final byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("업로드 파일을 읽을 수 없습니다.", e);
        }

        String sha256 = ImageHashUtils.sha256(original);
        long phash = ImageHashUtils.pHash(original);

        byte[] normalized = ImageNormalizer.normalizeToPng(original); // EXIF/ICC 정규화 → PNG
        String normalizedSha256 = ImageHashUtils.sha256(normalized);

        // 3) Flask 호출 (image+message) → 워터마크 이미지(base64) 수신
        ByteArrayResource imagePart = new ByteArrayResource(original) {
            @Override public String getFilename() { return file.getOriginalFilename(); }
        };
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", imagePart);
        builder.part("message", message);
        builder.part("taskId", taskId);
        builder.part("loginId", user.getLoginId());

        activeTaskService.registerTask(user.getLoginId(), taskId);
        WatermarkFlaskResponseDTO flask;
        try {
            flask = webClient.post()
                    .uri(flaskServerUrl + "/watermark-insert")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(WatermarkFlaskResponseDTO.class)
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            // HTTP 4xx/5xx 응답
            throw new ExternalServiceException(
                    "Flask HTTP error: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // 연결 실패/타임아웃 등
            throw new ExternalServiceException("Flask request failed: " + e.getMessage());
        } catch (Exception e) {
            throw new ExternalServiceException("Flask invocation failed");
        } finally {
            activeTaskService.deregisterTask(user.getLoginId());
        }

        if (flask == null || flask.getImage_base64() == null || flask.getImage_base64().isBlank()) {
            throw new ExternalServiceException("Flask 서버 응답이 비어 있습니다.");
        }

        byte[] watermarkedBytes;
        try {
            watermarkedBytes = Base64.getDecoder().decode(flask.getImage_base64());
        } catch (IllegalArgumentException e) {
            throw new ImageDecodingException("Flask 응답의 base64 디코딩 실패");
        }

        // 4) S3 업로드 (artifactId 기준 경로)
        String artifactId = UUID.randomUUID().toString();
        String baseKey = "watermarks/%d/%s/".formatted(userId, artifactId);
        String wmKey   = baseKey + "watermarked.png";
        String msgKey  = baseKey + "message.txt";

        String imageUrl = null;
        String msgUrl = null;

        try (InputStream wmIs = new ByteArrayInputStream(watermarkedBytes);
             InputStream msgIs = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8))) {

            imageUrl = amazonS3Service.uploadStream(wmIs, wmKey, "image/png");
            msgUrl = amazonS3Service.uploadStream(msgIs, msgKey, "text/plain");
        } catch (Exception e) {
            throw new StorageException("S3 업로드 실패", e);
        }

        // 5) DB 저장
        Watermark wm = Watermark.builder()
                .user(user)
                .artifactId(artifactId)
                .message(message)
                .sha256(sha256)
                .normalizedSha256(normalizedSha256)
                .phash(phash)
                .s3WatermarkedKey(imageUrl)
                .s3MessageKey(msgUrl)
                .fileName(flask.getFilename())
                .createdAt(LocalDateTime.now())
                .taskId(taskId)
                .build();

        watermarkRepository.save(wm);

        // 6) 응답
        return InsertResultDTO.builder()
                .artifactId(artifactId)
                .fileName(flask.getFilename())
                .s3WatermarkedKey(imageUrl)
                .message(message)
                .sha256(sha256)
                .normalizedSha256(normalizedSha256)
                .phash(phash)
                .createdAt(LocalDateTime.now())
                .taskId(taskId)
                .build();
        }
  
    public Page<InsertResultDTO> getAllResult(Long userId, Pageable pageable){
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (pageable == null) throw new IllegalArgumentException("pageable은 null일 수 없습니다.");
        return watermarkRepository.findByUser_UserId(userId, pageable)
                .map(InsertResultDTO::fromEntity);
    }

    public InsertResultDTO getSingleResult(Long userId, Long id){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Watermark mark = watermarkRepository.findByWatermarkIdAndUser(id, user)
                .orElseThrow(() -> new WatermarkNotFoundException(id, userId));

        return InsertResultDTO.fromEntity(mark);
    }

    public void deleteWatermark(Long userId, Long id){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        int deleted = watermarkRepository.deleteByWatermarkIdAndUser(id, user);
        if (deleted == 0) throw new WatermarkNotFoundException(id, userId);
    }
}
