package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeepfakeDetectionServiceTest {

    @InjectMocks
    private DeepfakeDetectionService deepfakeDetectionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeepfakeDetectionRepository deepfakeDetectionRepository;

    private User mockUser;
    private DeepfakeDetection detection1;
    private DeepfakeDetection detection2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .build();

        detection1 = DeepfakeDetection.builder()
                .deepfakeDetectionId(1L)
                .user(mockUser)
                .filePath("path1.mp4")
                .deepfakeResult(0.9f)
                .riskScore(0.8f)
                .detectedPart("{\"face\": true}")
                .createdAt(LocalDateTime.now())
                .build();

        detection2 = DeepfakeDetection.builder()
                .deepfakeDetectionId(2L)
                .user(mockUser)
                .filePath("path2.mp4")
                .deepfakeResult(0.3f)
                .riskScore(0.2f)
                .detectedPart("{\"face\": false}")
                .createdAt(LocalDateTime.now())
                .build();
    }


    @Test
    @DisplayName("전체 탐지 결과를 반환한다")
    void getAllResult() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(deepfakeDetectionRepository.findAllByUser(mockUser)).thenReturn(List.of(detection1, detection2));

        // when
        List<DeepfakeDetectionDTO> result = deepfakeDetectionService.getAllResult(1L);

        // then
        assertEquals(2, result.size());
        assertEquals("path1.mp4", result.get(0).getFilePath());
        assertEquals("path2.mp4", result.get(1).getFilePath());

        // repository 호출 여부 검증 (옵션)
        verify(deepfakeDetectionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("특정 탐지 결과를 반환한다")
    void getSingleResult_success() {
        Long detectionId = 1L;
        Long userId = 1L;

        User mockUser = User.builder().userId(userId).email("test@example.com").build();
        DeepfakeDetection mockDetection = DeepfakeDetection.builder()
                .deepfakeDetectionId(detectionId)
                .user(mockUser)
                .filePath("test/path.mp4")
                .deepfakeResult(0.85f)
                .riskScore(0.75f)
                .detectedPart("{\"face\": true}")
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        Mockito.when(deepfakeDetectionRepository.findByIdAndUser(detectionId, mockUser)).thenReturn(Optional.of(mockDetection));

        DeepfakeDetectionDTO result = deepfakeDetectionService.getSingleResult(userId, detectionId);

        assertEquals("test/path.mp4", result.getFilePath());
        assertEquals(0.85f, result.getDeepfakeResult());
    }

    @Test
    @DisplayName("탐지 결과를 삭제한다")
    void deleteResult() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(deepfakeDetectionRepository.findByIdAndUser(1L, mockUser)).thenReturn(Optional.of(detection1));

        assertDoesNotThrow(() -> deepfakeDetectionService.deleteResult(1L, 1L));
        verify(deepfakeDetectionRepository, times(1)).delete(detection1);
    }
}