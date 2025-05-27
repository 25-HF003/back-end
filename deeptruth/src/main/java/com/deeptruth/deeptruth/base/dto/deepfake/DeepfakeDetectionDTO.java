package com.deeptruth.deeptruth.base.dto.deepfake;

import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeepfakeDetectionDTO {
    private Long id;
    private String filePath;
    private Float deepfakeResult;
    private Float riskScore;
    private String detectedPart;
    private LocalDateTime createdAt;

    public static DeepfakeDetectionDTO fromEntity(DeepfakeDetection entity) {
        return DeepfakeDetectionDTO.builder()
                .id(entity.getDeepfakeDetectionId())
                .filePath(entity.getFilePath())
                .deepfakeResult(entity.getDeepfakeResult())
                .riskScore(entity.getRiskScore())
                .detectedPart(entity.getDetectedPart())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
