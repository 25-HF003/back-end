package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.base.dto.noise.NoiseFlaskResponseDTO;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.Noise;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NoiseService {

    private final NoiseRepository noiseRepository;
    private final UserRepository userRepository;
    private final AmazonS3Service amazonS3Service;

    public NoiseDTO createNoise(Long userId, NoiseFlaskResponseDTO dto, String originFileName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Flask 응답 검증
        if (dto == null)
            throw new InvalidNoiseResponseException("response is null");
        if (dto.getAttackSuccess() == null)
            throw new InvalidNoiseResponseException("attackSuccess is null");
        if (dto.getOriginalFilePath() == null || dto.getOriginalFilePath().isBlank())
            throw new InvalidNoiseResponseException("originalFilePath is blank");
        if (dto.getProcessedFilePath() == null || dto.getProcessedFilePath().isBlank())
            throw new InvalidNoiseResponseException("processedFilePath is blank");

        log.info("📍 NoiseService - 받은 데이터:");
        log.info("- originalFilePath: {}", dto.getOriginalFilePath().startsWith("http") ? "S3 URL" : "Base64");
        log.info("- processedFilePath: {}", dto.getProcessedFilePath().startsWith("http") ? "S3 URL" : "Base64");

        String userFileName = generateFileName(originFileName);

        // Entity 생성 및 저장
        Noise noise = Noise.builder()
                .user(user)
                .originalFileName(originFileName)  // 원본 파일명
                .fileName(userFileName)  // 저장용 파일명
                .originalFilePath(dto.getOriginalFilePath())
                .processedFilePath(dto.getProcessedFilePath())
                .epsilon(dto.getEpsilon())
                .attackSuccess(dto.getAttackSuccess())
                .originalPrediction(dto.getOriginalPrediction())
                .adversarialPrediction(dto.getAdversarialPrediction())
                .originalConfidence(dto.getOriginalConfidence())
                .adversarialConfidence(dto.getAdversarialConfidence())
                .build();

        noiseRepository.save(noise);
        return NoiseDTO.fromEntityWithFlaskData(noise, dto);
    }

    // S3 업로드 메소드
    public String uploadBase64ImageToS3(String base64Image, Long userId, String type, String originalFileName) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new ImageDecodingException("empty string");
        }

        // data:image/png;base64, 제거
        String cleanBase64 = base64Image;
        if (base64Image.startsWith("data:image/")) {
            cleanBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
        }

        final byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(cleanBase64);
            log.info("Base64 디코딩 성공, 크기: {} bytes", decodedBytes.length);
        } catch (IllegalArgumentException e) {
            String preview = base64Image.length() > 50 ? base64Image.substring(0, 50) + "..." : base64Image;
            log.error("유효하지 않은 Base64 문자 발견: [{}]", preview);
            throw new ImageDecodingException("Failed to decode Base64 image: Invalid Base64 characters detected");
        }

        try (InputStream inputStream = new ByteArrayInputStream(decodedBytes)) {
            // S3에는 UUID로 저장 (충돌 방지)
            String key = "noise/" + userId + "/" + type + "/" + UUID.randomUUID() + ".jpg";
            String result = amazonS3Service.uploadBase64Image(inputStream, key);
            log.info("S3 업로드 성공: {}", result);
            return result;
        } catch (IOException e) {
            log.error("InputStream 처리 실패: {}", e.getMessage());
            throw new StorageException("failed to process image stream", e);
        } catch (Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new StorageException("failed to upload image to S3", e);
        }
    }

    public Page<NoiseDTO> getAllResult(Long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return noiseRepository.findByUser_UserId(userId, pageable)
                .map(NoiseDTO::fromEntity);
    }

    public NoiseDTO getSingleResult(Long userId, Long noiseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Noise noise = noiseRepository.findByNoiseIdAndUser(noiseId, user)
                .orElseThrow(() -> new NoiseNotFoundException(noiseId, userId));

        return NoiseDTO.fromEntity(noise);
    }

    public void deleteResult(Long userId, Long noiseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        int deleted = noiseRepository.deleteByNoiseIdAndUser(noiseId, user);
        if (deleted == 0) {
            throw new NoiseNotFoundException(noiseId, userId);
        }
    }

    public List<NoiseDTO> getUserNoiseHistory(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자가 존재하지 않습니다.");
        }

        List<Noise> noises = noiseRepository.findAllByUser_UserId(userId);
        return noises.stream()
                .map(NoiseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private String generateFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "noise_result.jpg";
        }

        // 확장자 추출
        int dotIndex = originalFileName.lastIndexOf('.');
        String extension = dotIndex != -1 ? originalFileName.substring(dotIndex) : ".jpg";

        // 확장자 검증
        if (extension.length() > 5 || extension.length() <= 1) {
            extension = ".jpg";
        }

        // 파일명 정리
        String baseName = dotIndex != -1 ? originalFileName.substring(0, dotIndex) : originalFileName;
        baseName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_");

        if (baseName.length() > 15) {
            baseName = baseName.substring(0, 15);
        }

        return baseName + "_noise" + extension.toLowerCase();
    }

}