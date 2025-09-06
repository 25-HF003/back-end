package com.deeptruth.deeptruth.base.dto.watermark;

import com.deeptruth.deeptruth.entity.Watermark;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class WatermarkDTO {
    private Long id;
    private String watermarkedFilePath;
    private String fileName;
    private LocalDateTime createdAt;
    private String taskId;

    public static WatermarkDTO fromEntity(Watermark entity){
        return WatermarkDTO.builder()
                .id(entity.getWatermarkId())
                .fileName(entity.getFileName())
                .createdAt(entity.getCreatedAt())
                .taskId(entity.getTaskId())
                .build();
    }
}
