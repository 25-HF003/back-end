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
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DeepfakeDetectionDTO {
    private Long id;
    private String taskId;
    private String filePath;
    private LocalDateTime createdAt;
    private DeepfakeResult result;

    private Float scoreWeighted;
    private Float thresholdTau;
    private Float frameVoteRatio;

    private Float averageConfidence;
    private Float maxConfidence;
    private Float medianConfidence;
    private Float varianceConfidence;

    private Integer framesProcessed;
    private Float processingTimeSec;

    private Float fpsProcessed;
    private Float msPerSample;
    private Float targetFpsUsed;
    private Float maxLatencyMsUsed;
    private Boolean speedOk;

    private Float temporalDeltaMean;
    private Float temporalDeltaStd;
    private Float ttaMean;
    private Float ttaStd;

    private String timeseriesJson;

    private Float speedScore;
    private Float stabilityScore;

    private DeepfakeMode mode;
    private DeepfakeDetector detector;
    private Boolean useTta;
    private Boolean useIllum;
    private Integer smoothWindow;
    private Integer minFace;
    private Integer sampleCount;

    private List<BulletDTO> stabilityBullets;
    private List<BulletDTO> speedBullets;

    public static DeepfakeDetectionDTO fromEntity(DeepfakeDetection entity,
                                                  List<BulletDTO> stability,
                                                  List<BulletDTO> speed) {
        return DeepfakeDetectionDTO.builder()
                .id(entity.getDeepfakeDetectionId())
                .taskId(entity.getTaskId())
                .filePath(entity.getFilePath())
                .result(entity.getResult())
                .createdAt(entity.getCreatedAt())
                .scoreWeighted(entity.getScoreWeighted())
                .thresholdTau(entity.getThresholdTau())
                .frameVoteRatio(entity.getFrameVoteRatio())
                .averageConfidence(entity.getAverageConfidence())
                .maxConfidence(entity.getMaxConfidence())
                .medianConfidence(entity.getMedianConfidence())
                .varianceConfidence(entity.getVarianceConfidence())
                .framesProcessed(entity.getFramesProcessed())
                .processingTimeSec(entity.getProcessingTimeSec())
                .fpsProcessed(entity.getFpsProcessed())
                .msPerSample(entity.getMsPerSample())
                .targetFpsUsed(entity.getTargetFpsUsed())
                .maxLatencyMsUsed(entity.getMaxLatencyMsUsed())
                .speedOk(entity.getSpeedOk())
                .temporalDeltaMean(entity.getTemporalDeltaMean())
                .temporalDeltaStd(entity.getTemporalDeltaStd())
                .ttaMean(entity.getTtaMean())
                .ttaStd(entity.getTtaStd())
                .timeseriesJson(entity.getTimeseriesJson())
                .speedScore(entity.getSpeedScore())
                .stabilityScore(entity.getStabilityScore())
                .mode(entity.getMode())
                .useTta(entity.getUseTta())
                .useIllum(entity.getUseIllum())
                .detector(entity.getDetector())
                .smoothWindow(entity.getSmoothWindow())
                .minFace(entity.getMinFace())
                .sampleCount(entity.getSampleCount())
                .speedBullets(speed)
                .stabilityBullets(stability)
                .build();
    }
}
