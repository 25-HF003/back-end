package com.deeptruth.deeptruth.base.dto.deepfake;

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
}
