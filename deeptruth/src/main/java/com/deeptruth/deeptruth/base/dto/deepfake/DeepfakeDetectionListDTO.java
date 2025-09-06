package com.deeptruth.deeptruth.base.dto.deepfake;

import com.deeptruth.deeptruth.base.Enum.DeepfakeMode;
import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeepfakeDetectionListDTO {
    private Long id;
    private String taskId;
    private String filePath;
    private LocalDateTime createdAt;
    private DeepfakeResult result;
    private DeepfakeMode deepfakeMode;

    public static DeepfakeDetectionListDTO fromEntity(DeepfakeDetection entity){
        return DeepfakeDetectionListDTO.builder()
                .id(entity.getDeepfakeDetectionId())
                .taskId(entity.getTaskId())
                .filePath(entity.getFilePath())
                .result(entity.getResult())
                .deepfakeMode(entity.getMode())
                .build();
    }
}
