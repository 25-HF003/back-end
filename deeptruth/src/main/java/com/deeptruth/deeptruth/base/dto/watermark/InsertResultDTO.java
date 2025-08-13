package com.deeptruth.deeptruth.base.dto.watermark;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InsertResultDTO {
    private String artifactId;
    private String watermarkedKey;
    private String message;
}
