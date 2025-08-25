package com.deeptruth.deeptruth.base.dto.watermarkDetection;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatermarkDetectionFlaskResponseDTO {
    private String basename;
    private Double bit_accuracy;     // Flask snake_case 그대로
    private String detected_at;
    private String image_base64;     // 정확도 < 임계 시에만 존재
}
