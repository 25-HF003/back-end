package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import static org.mockito.BDDMockito.then;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class DeepfakeDetectionServiceTest {

    @InjectMocks
    private DeepfakeDetectionService deepfakeDetectionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeepfakeDetectionRepository deepfakeDetectionRepository;

    @Mock
    private AmazonS3Service amazonS3Service;

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
                .result(DeepfakeResult.FAKE)
                .createdAt(LocalDateTime.now())
                .build();

        detection2 = DeepfakeDetection.builder()
                .deepfakeDetectionId(2L)
                .user(mockUser)
                .filePath("path2.mp4")
                .result(DeepfakeResult.REAL)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("영상을 업로드하고 deepfake 반환값을 받는다. ")
    void uploadVideo_ShouldReturnS3Url() throws IOException {
        // given
        Long userId = 1L;
        MultipartFile multipartFile = new MockMultipartFile("file", "video.mp4", "video/mp4", "video content".getBytes());
        String uploadedUrl = "https://s3.amazonaws.com/deepfake/video.mp4";

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(amazonS3Service.uploadFile(anyString(), any(MultipartFile.class))).willReturn(uploadedUrl);

        // when
        DeepfakeDetectionDTO result = deepfakeDetectionService.uploadVideo(userId, multipartFile);

        // then
        assertNotNull(result);
        assertEquals(uploadedUrl, result.getFilePath());
        assertEquals(DeepfakeResult.FAKE, result.getResult()); // 하드코딩 값 기준

        then(userRepository).should().findById(userId);
        then(amazonS3Service).should().uploadFile("deepfake", multipartFile);
        verify(deepfakeDetectionRepository, times(1)).save(any(DeepfakeDetection.class));
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
                .result(DeepfakeResult.FAKE)
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        Mockito.when(deepfakeDetectionRepository.findByDeepfakeDetectionIdAndUser(detectionId, mockUser)).thenReturn(Optional.of(mockDetection));

        DeepfakeDetectionDTO result = deepfakeDetectionService.getSingleResult(userId, detectionId);

        assertEquals("test/path.mp4", result.getFilePath());
        assertEquals(DeepfakeResult.FAKE, result.getResult());
    }

    @Test
    @DisplayName("탐지 결과를 삭제한다")
    void deleteResult() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(deepfakeDetectionRepository.findByDeepfakeDetectionIdAndUser(1L, mockUser)).thenReturn(Optional.of(detection1));

        assertDoesNotThrow(() -> deepfakeDetectionService.deleteResult(1L, 1L));
        verify(deepfakeDetectionRepository, times(1)).deleteByDeepfakeDetectionIdAndUser(1L, mockUser);
    }
}