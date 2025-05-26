package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
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
        DeepfakeDetectionDTO dto1 = DeepfakeDetectionDTO.builder()
                .id(1L)
                .filePath("path/to/video1.mp4")
                .deepfakeResult(0.85f)
                .riskScore(0.90f)
                .detectedPart("face")
                .createdAt(LocalDateTime.now())
                .build();

        DeepfakeDetectionDTO dto2 = DeepfakeDetectionDTO.builder()
                .id(2L)
                .filePath("path/to/video2.mp4")
                .deepfakeResult(0.15f)
                .riskScore(0.20f)
                .detectedPart("mouth")
                .createdAt(LocalDateTime.now())
                .build();

        List<DeepfakeDetectionDTO> mockResults = Arrays.asList(dto1, dto2);

        // 실제 구현에서 Repository에서 데이터를 받아오거나 매핑한다고 가정
        when(deepfakeDetectionRepository.findAllAsDTO()).thenReturn(mockResults);

        // when
        List<DeepfakeDetectionDTO> results = deepfakeDetectionService.getAllResult();

        // then
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("path/to/video1.mp4", results.get(0).getFilePath());
        assertEquals("face", results.get(0).getDetectedPart());
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