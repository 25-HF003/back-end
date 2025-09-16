package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.config.JwtAuthenticationFilter;
import com.deeptruth.deeptruth.config.SecurityConfig;
import com.deeptruth.deeptruth.service.DeepfakeDetectionService;
import com.deeptruth.deeptruth.testsecurity.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = DeepfakeDetectionController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,
                        JwtAuthenticationFilter.class
                })
        }
)@AutoConfigureMockMvc(addFilters = false)
public class DeepfakeDetectionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private DeepfakeDetectionService deepfakeDetectionService;

    @Test
    @DisplayName("POST /api/deepfake - 딥페이크 탐지 성공")
    @WithMockCustomUser(userId = 7L, role = "USER")
    void uploadVideo_ShouldReturn200AndResponseDTO() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "bytes".getBytes()
        );


        DeepfakeDetectionDTO dto = DeepfakeDetectionDTO.builder()
                .taskId("task-123")
                .filePath("s3://fake/video.mp4")
                .result(DeepfakeResult.FAKE)
                .build();
        when(deepfakeDetectionService.createDetection(eq(7L), any(), anyMap()))
                .thenReturn(dto);

        // when & then
        mockMvc.perform(multipart("/api/deepfake")
                        .file(file)
                        .param("taskId", "task-123")
                        .param("mode", "video")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("딥페이크 탐지 결과 수신 성공"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("GET /api/deepfake - 리스트 조회 성공")
    @WithMockCustomUser(userId = 7L, role = "USER")
    void getDeepfakeDetections_success() throws Exception {
        // given
        DeepfakeDetectionDTO dto = DeepfakeDetectionDTO.builder()
                .taskId("task-abc")
                .filePath("s3://bucket/sample.jpg")
                .result(DeepfakeResult.REAL)
                .build();

        // when
        when(deepfakeDetectionService.getSingleResult(eq(7L), eq(10L)))
                .thenReturn(dto);

        // then
        mockMvc.perform(get("/api/deepfake/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("딥페이크 탐지 결과 조회 성공"))
                .andExpect(jsonPath("$.data.taskId").value("task-abc"))
                .andExpect(jsonPath("$.data.result").value("REAL"));
    }

    @Test
    @DisplayName("GET /api/deepfake - 페이지 조회 성공")
    @WithMockCustomUser(userId = 7L, role = "USER")
    void getSingleDeepfakeDetection_success() throws Exception {
        // when
        when(deepfakeDetectionService.getAllResult(eq(7L), any()))
                .thenAnswer(inv -> Page.empty());

        // then
        mockMvc.perform(get("/api/deepfake"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("딥페이크 탐지 결과 전체 조회 성공"))
                .andExpect(jsonPath("$.data").exists());
    }


    @Test
    @DisplayName("DELETE /api/deepfake/{id} - 삭제 성공")
    @WithMockCustomUser(userId = 7L, role = "USER")
    void deleteDeepfakeDetection_success() throws Exception {
        mockMvc.perform(delete("/api/deepfake/{id}", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("딥페이크 탐지 결과 삭제 성공"));
    }
}
