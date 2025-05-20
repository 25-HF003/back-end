package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.service.DeepfakeDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

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
    @DisplayName("딥페이크 탐지 결과를 조회한다")
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void getDeepfakeDetections_success() throws Exception {
        DeepfakeDetectionDTO dto = DeepfakeDetectionDTO.builder()
                .id(1L)
                .filePath("test/path.mp4")
                .deepfakeResult(0.85f)
                .riskScore(0.75f)
                .detectedPart("{\"face\": true}")
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(deepfakeDetectionService.getAllResult())
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/deepfake"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("딥페이크 탐지 결과 전체 조회 성공"))
                .andExpect(jsonPath("$.data[0].filePath").value("test/path.mp4"));
    }

    @Test
    @DisplayName("딥페이크 탐지 결과를 삭제한다")
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void deleteDeepfakeDetection_success() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/deepfake/{id}", id)
                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("딥페이크 탐지 결과 삭제 성공"));
    }
}
