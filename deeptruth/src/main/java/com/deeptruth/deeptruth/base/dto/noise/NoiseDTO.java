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
    private String originalFilePath;
    private String processedFilePath;
    private Float epsilon;
    private Boolean attackSuccess;
    private String originalPrediction;
    private String adversarialPrediction;
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
                .originalFilePath(entity.getOriginalFilePath())
                .processedFilePath(entity.getProcessedFilePath())
                .epsilon(entity.getEpsilon())
                .createdAt(entity.getCreatedAt())
                .attackSuccess(entity.getAttackSuccess())
                .originalPrediction(entity.getOriginalPrediction())
                .adversarialPrediction(entity.getAdversarialPrediction())
                .build();
    }

    // Flask 응답 포함
    public static NoiseDTO fromEntityWithFlaskData(Noise entity, NoiseFlaskResponseDTO flaskResponse) {
        return NoiseDTO.builder()
                .noiseId(entity.getNoiseId())
                .userId(entity.getUser().getUserId())
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
                .build();
    }

    public String getStyleTransform() {
        if (originalPrediction != null && adversarialPrediction != null) {
            return originalPrediction + " → " + adversarialPrediction;
        }
        return null;
    }
}