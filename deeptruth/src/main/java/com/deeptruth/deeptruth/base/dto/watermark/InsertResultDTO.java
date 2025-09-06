package com.deeptruth.deeptruth.base.dto.watermark;

import com.deeptruth.deeptruth.entity.Watermark;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InsertResultDTO {
    private Long watermarkId;
    private String artifactId;
    private String fileName;
    private String s3WatermarkedKey;
    private String message;
    private String sha256;
    private String normalizedSha256;
    private Long phash;
    private LocalDateTime createdAt;
    private String taskId;

    public static InsertResultDTO fromEntity(Watermark entity){
        return InsertResultDTO.builder()
                .watermarkId(entity.getWatermarkId())
                .artifactId(entity.getArtifactId())
                .fileName(entity.getFileName())
                .s3WatermarkedKey(entity.getS3WatermarkedKey())
                .message(entity.getMessage())
                .sha256(entity.getSha256())
                .normalizedSha256(entity.getNormalizedSha256())
                .phash(entity.getPhash())
                .createdAt(entity.getCreatedAt())
                .taskId(entity.getTaskId())
                .build();
    }
}
