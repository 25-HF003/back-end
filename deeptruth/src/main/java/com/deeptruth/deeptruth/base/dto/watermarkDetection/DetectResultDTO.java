package com.deeptruth.deeptruth.base.dto.watermarkDetection;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetectResultDTO {
    private String artifactId;
    private String matchMethod;      // SHA256 / NORMALIZED_SHA256 / PHASH
    private Integer phashDistance;   // PHASH일 때만
    private Double bitAccuracy;      // (%)
    private String detectedAt;
    private String uploadedImageBase64; // 임계 미달 시만
    private String basename;
}