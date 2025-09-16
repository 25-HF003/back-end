package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.watermarkDetection.DetectResultDTO;
import com.deeptruth.deeptruth.config.JwtAuthenticationFilter;
import com.deeptruth.deeptruth.config.SecurityConfig;
import com.deeptruth.deeptruth.service.WatermarkDetectionService;
import com.deeptruth.deeptruth.testsecurity.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(
        controllers = WatermarkDetectionController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,
                        JwtAuthenticationFilter.class
                })
        }
)@AutoConfigureMockMvc(addFilters = false)
class WatermarkDetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WatermarkDetectionService detectionService;

    @Test
    @DisplayName("POST /api/watermark/detection - 워터마크 탐지 성공")
    @WithMockCustomUser(userId = 7L, role = "USER")
    void detect_success() throws Exception {
        // given
        DetectResultDTO dto = DetectResultDTO.builder()
                .artifactId("art-123")
                .matchMethod("PHASH")
                .phashDistance(0)
                .bitAccuracy(0.92d)
                .detectedAt("2025-01-01T00:00:00Z")
                .basename("input.png")
                .taskId("task-1")
                .build();

        Mockito.when(detectionService.detect(any(Long.class), any(), eq("task-1")))
                .thenReturn(dto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "input.png", "image/png", "pngbytes".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/watermark/detection")
                        .file(file)
                        .param("taskId", "task-1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // ResponseDTO 공통 필드
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("워터마크 탐지 성공"))
                // data 필드
                .andExpect(jsonPath("$.data.artifactId").value("art-123"))
                .andExpect(jsonPath("$.data.matchMethod").value("PHASH"))
                .andExpect(jsonPath("$.data.taskId").value("task-1"));
    }
}