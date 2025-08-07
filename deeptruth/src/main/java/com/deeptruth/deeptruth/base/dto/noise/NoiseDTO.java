package com.deeptruth.deeptruth.base.dto.noise;

import com.deeptruth.deeptruth.entity.Noise;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoiseDTO {
    private Long noiseId;
    private Long userId;
    private String originalFilePath;
    private String processedFilePath;
    private Float epsilon;
    private LocalDateTime createdAt;

    public static NoiseDTO fromEntity(Noise entity) {
        return NoiseDTO.builder()
                .noiseId(entity.getNoiseId())
                .userId(entity.getUser().getUserId())
                .originalFilePath(entity.getOriginalFilePath())
                .processedFilePath(entity.getProcessedFilePath())
                .epsilon(entity.getEpsilon())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}