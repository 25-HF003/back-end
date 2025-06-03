package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.service.DeepfakeDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeepfakeDetectionController.class)
public class DeepfakeDetectionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeepfakeDetectionService deepfakeDetectionService;

    @Test
    @DisplayName("영상을 업로드한다")
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void uploadVideo_ShouldReturn200AndResponseDTO() throws Exception {
        // given
        Long userId = 1L;
        Float deepfakeResult = 0.7F;
        Float riskScore = 0.7F;
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", "fake video content".getBytes());

        DeepfakeDetectionDTO mockDto = DeepfakeDetectionDTO.builder()
                .filePath("https://s3.amazonaws.com/deepfake/video.mp4")
                .deepfakeResult(deepfakeResult)
                .riskScore(riskScore)
                .build();

        given(deepfakeDetectionService.uploadVideo(eq(userId), any(MultipartFile.class)))
                .willReturn(mockDto);


        // when & then
        mockMvc.perform(multipart("/deepfake")
                        .file(file)
                        .with(csrf())
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("딥페이크 탐지 영상 업로드 성공"))
                .andExpect(jsonPath("$.data.filePath").value("https://s3.amazonaws.com/deepfake/video.mp4"))
                .andExpect(jsonPath("$.data.deepfakeResult").value(deepfakeResult))
                .andExpect(jsonPath("$.data.riskScore").value(riskScore));
    }

    @Test
    @DisplayName("딥페이크 탐지 결과를 조회한다")
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void getDeepfakeDetections_success() throws Exception {
        Long userId = 1L;
        DeepfakeDetectionDTO dto = DeepfakeDetectionDTO.builder()
                .id(1L)
                .filePath("test/path.mp4")
                .deepfakeResult(0.85f)
                .riskScore(0.75f)
                .detectedPart("{\"face\": true}")
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(deepfakeDetectionService.getAllResult(userId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/deepfake")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("딥페이크 탐지 결과 전체 조회 성공"))
                .andExpect(jsonPath("$.data[0].filePath").value("test/path.mp4"));
    }

    @Test
    @DisplayName("특정 딥페이크 탐지 결과를 조회한다")
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void getSingleDeepfakeDetection_success() throws Exception {
        Long id = 1L;
        Long userId = 1L;

        DeepfakeDetectionDTO dto = DeepfakeDetectionDTO.builder()
                .id(id)
                .filePath("test/path.mp4")
                .deepfakeResult(0.85f)
                .riskScore(0.75f)
                .detectedPart("{\"face\": true}")
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(deepfakeDetectionService.getSingleResult(userId, id))
                .thenReturn(dto);

        mockMvc.perform(get("/deepfake/{id}", id)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("딥페이크 탐지 결과 조회 성공"))
                .andExpect(jsonPath("$.data.filePath").value("test/path.mp4"));
    }


    @Test
    @DisplayName("딥페이크 탐지 결과를 삭제한다")
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void deleteDeepfakeDetection_success() throws Exception {
        Long id = 1L;
        Long userId = 1L;

        mockMvc.perform(delete("/deepfake/{id}", id)
                        .param("userId", String.valueOf(userId))
                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("딥페이크 탐지 결과 삭제 성공"));
    }
}
