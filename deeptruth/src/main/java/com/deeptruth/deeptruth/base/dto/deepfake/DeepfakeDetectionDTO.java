package com.deeptruth.deeptruth.base.dto.deepfake;

import com.deeptruth.deeptruth.base.Enum.DeepfakeDetector;
import com.deeptruth.deeptruth.base.Enum.DeepfakeMode;
import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeepfakeDetectionDTO {
    private Long id;
    private String filePath;
    private DeepfakeResult result;
    private Float averageConfidence;
    private Float maxConfidence;
    private LocalDateTime createdAt;
    private DeepfakeMode mode;
    private Boolean useTta;
    private Boolean useIllum;
    private DeepfakeDetector detector;
    private Integer smoothWindow;
    private Integer minFace;
    private Integer sampleCount;

    public static DeepfakeDetectionDTO fromEntity(DeepfakeDetection entity) {
        return DeepfakeDetectionDTO.builder()
                .id(entity.getDeepfakeDetectionId())
                .filePath(entity.getFilePath())
                .result(entity.getResult())
                .averageConfidence(entity.getAverageConfidence())
                .maxConfidence(entity.getMaxConfidence())
                .createdAt(entity.getCreatedAt())
                .mode(entity.getMode())
                .useTta(entity.getUseTta())
                .useIllum(entity.getUseIllum())
                .detector(entity.getDetector())
                .smoothWindow(entity.getSmoothWindow())
                .minFace(entity.getMinFace())
                .sampleCount(entity.getSampleCount())
                .build();
    }
}
