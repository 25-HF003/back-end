package com.deeptruth.deeptruth.base.dto.watermark;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InsertResultDTO {
    private String artifactId;
    private String fileName;
    private String s3WatermarkedKey;
    private String message;
    private String sha256;
    private String normalizedSha256;
    private Long phash;
    private LocalDateTime createdAt;

}
