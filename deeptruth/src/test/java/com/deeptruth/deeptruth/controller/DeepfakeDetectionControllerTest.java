package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
        Mockito.when(deepfakeDetectionService.getAllDetectionsForCurrentUser())
                .thenReturn(List.of(
                        new DeepfakeDetectionDto(1L, "test.mp4", 0.85f, 0.6f, "{\"face\": true}", "2025-05-18T12:00:00")
                ));

        mockMvc.perform(get("/deepfake"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filePath").value("test.mp4"))
                .andExpect(jsonPath("$[0].deepfakeResult").value(0.85));
    }

    @Test
    @DisplayName("딥페이크 탐지 결과를 삭제한다")
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void deleteDeepfakeDetection_success() throws Exception {
        Long detectionId = 1L;

        mockMvc.perform(delete("/deepfake/" + detectionId))
                .andExpect(status().isNoContent());
    }
}
