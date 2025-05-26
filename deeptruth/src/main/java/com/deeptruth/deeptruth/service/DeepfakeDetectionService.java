package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeepfakeDetectionService {

    private final DeepfakeDetectionRepository deepfakeDetectionRepository;

    public List<DeepfakeDetectionDTO> getAllResult(){
        List<DeepfakeDetection> entities = deepfakeDetectionRepository.findAll();

        return entities.stream()
                .map(entity -> DeepfakeDetectionDTO.builder()
                        .id(entity.getDeepfakeDetectionId())
                        .filePath(entity.getFilePath())
                        .deepfakeResult(entity.getDeepfakeResult())
                        .riskScore(entity.getRiskScore())
                        .detectedPart(entity.getDetectedPart())
                        .createdAt(entity.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public void deleteResult(Long id){
        deepfakeDetectionRepository.deleteById(id);
    }
}
