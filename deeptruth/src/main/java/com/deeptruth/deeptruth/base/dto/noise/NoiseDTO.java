package com.deeptruth.deeptruth.base.dto.noise;

import com.deeptruth.deeptruth.entity.Noise;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoiseDTO {
    private Long noiseId;
    private Long userId;
    private String fileName;
    private String originalFileName;
    private String originalFilePath;
    private String processedFilePath;
    private Float epsilon;
    private Boolean attackSuccess;
    private String originalPrediction;
    private String adversarialPrediction;
    private String mode;                 // "auto" 또는 "precision"
    private Integer level;               // 1-4 (정밀 모드만)
    private String modeDescription;      // "자동 모드" 또는 "정밀 모드"
    private LocalDateTime createdAt;

    // 프론트 표시용
    private String originalImageBase64;
    private String processedImageBase64;
    private String originalConfidence;
    private String adversarialConfidence;
    private String confidenceDrop;

    public static NoiseDTO fromEntity(Noise entity) {
        return NoiseDTO.builder()
                .noiseId(entity.getNoiseId())
                .userId(entity.getUser().getUserId())
                .fileName(entity.getFileName())
                .originalFileName(entity.getOriginalFileName())
                .originalFilePath(entity.getOriginalFilePath())
                .processedFilePath(entity.getProcessedFilePath())
                .epsilon(entity.getEpsilon())
                .createdAt(entity.getCreatedAt())
                .attackSuccess(entity.getAttackSuccess())
                .originalPrediction(entity.getOriginalPrediction())
                .adversarialPrediction(entity.getAdversarialPrediction())
                .originalConfidence(entity.getOriginalConfidence())
                .adversarialConfidence(entity.getAdversarialConfidence())
                .mode(entity.getMode())
                .level(entity.getLevel())
                .modeDescription(getModeDescription(entity.getMode(), entity.getLevel()))
                .build();
    }

    // Flask 응답 포함
    public static NoiseDTO fromEntityWithFlaskData(Noise entity, NoiseFlaskResponseDTO flaskResponse) {
        return NoiseDTO.builder()
                .noiseId(entity.getNoiseId())
                .userId(entity.getUser().getUserId())
                .fileName(entity.getFileName())
                .originalFileName(entity.getOriginalFileName())
                .originalFilePath(entity.getOriginalFilePath())
                .processedFilePath(entity.getProcessedFilePath())
                .epsilon(entity.getEpsilon())
                .createdAt(entity.getCreatedAt())
                .attackSuccess(entity.getAttackSuccess())
                .originalPrediction(entity.getOriginalPrediction())
                .adversarialPrediction(entity.getAdversarialPrediction())
                // Flask 응답 데이터
                .originalImageBase64(flaskResponse.getOriginalFilePath())
                .processedImageBase64(flaskResponse.getProcessedFilePath())
                .originalConfidence(flaskResponse.getOriginalConfidence())
                .adversarialConfidence(flaskResponse.getAdversarialConfidence())
                .confidenceDrop(flaskResponse.getConfidenceDrop())
                .mode(flaskResponse.getMode())
                .level(flaskResponse.getLevel())
                .modeDescription(getModeDescription(flaskResponse.getMode(), flaskResponse.getLevel()))
                .build();
    }

    public String getStyleTransform() {
        if (originalPrediction != null && adversarialPrediction != null) {
            return originalPrediction + " → " + adversarialPrediction;
        }
        return null;
    }

    // 모드 설명 생성 헬퍼 메서드 추가
    private static String getModeDescription(String mode, Integer level) {
        if ("precision".equals(mode) && level != null) {
            switch (level) {
                case 1:
                    return "정밀 모드 (약함)";
                case 2:
                    return "정밀 모드 (보통)";
                case 3:
                    return "정밀 모드 (강함)";
                case 4:
                    return "정밀 모드 (매우 강함)";
                default:
                    return "정밀 모드";
            }
        }
        return "자동 모드";
    }

}