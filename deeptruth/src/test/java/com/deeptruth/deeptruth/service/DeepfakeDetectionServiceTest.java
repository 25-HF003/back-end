package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeepfakeDetectionServiceTest {

    @InjectMocks
    private DeepfakeDetectionService deepfakeDetectionService;

    @Mock
    private DeepfakeDetectionRepository deepfakeDetectionRepository;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllResult() {
        // given
        DeepfakeDetection entity1 = DeepfakeDetection.builder()
                .deepfakeDetectionId(1L)
                .filePath("file1.mp4")
                .deepfakeResult(0.1f)
                .riskScore(0.2f)
                .detectedPart("{}")
                .createdAt(LocalDateTime.now())
                .build();

        DeepfakeDetection entity2 = DeepfakeDetection.builder()
                .deepfakeDetectionId(2L)
                .filePath("file2.mp4")
                .deepfakeResult(0.3f)
                .riskScore(0.4f)
                .detectedPart("{}")
                .createdAt(LocalDateTime.now())
                .build();

        List<DeepfakeDetection> mockList = Arrays.asList(entity1, entity2);

        // stub 설정: repository.findAll() 호출 시 mockList 반환
        when(deepfakeDetectionRepository.findAll()).thenReturn(mockList);

        // when
        List<DeepfakeDetectionDTO> result = deepfakeDetectionService.getAllResult();

        // then
        assertEquals(2, result.size());
        assertEquals(entity1.getDeepfakeDetectionId(), result.get(0).getId());
        assertEquals(entity2.getDeepfakeDetectionId(), result.get(1).getId());

        // repository 호출 여부 검증 (옵션)
        verify(deepfakeDetectionRepository, times(1)).findAll();
    }

    @Test
    void deleteResult() {
        // given
        Long id = 1L;

        // when
        deepfakeDetectionService.deleteResult(id);

        // then
        verify(deepfakeDetectionRepository, times(1)).deleteById(id);
    }
}