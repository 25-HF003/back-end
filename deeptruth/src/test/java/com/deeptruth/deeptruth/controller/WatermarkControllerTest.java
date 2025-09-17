package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.watermark.InsertResultDTO;
import com.deeptruth.deeptruth.config.JwtAuthenticationFilter;
import com.deeptruth.deeptruth.config.SecurityConfig;
import com.deeptruth.deeptruth.service.UserService;
import com.deeptruth.deeptruth.service.WatermarkService;
import com.deeptruth.deeptruth.testsecurity.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;


import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = WatermarkController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,
                        JwtAuthenticationFilter.class
                })
        }
)@AutoConfigureMockMvc(addFilters = false)
public class WatermarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private WatermarkService watermarkService;
    @MockitoBean private UserService userService;
    @MockitoBean private WebClient webClient;

    @Test
    @DisplayName("POST /api/watermark - 워터마크 삽입 성공")
    @WithMockCustomUser(userId = 7L, role = "USER")
    void insertWatermark_success() throws Exception {
        Long userId = 7L;

        InsertResultDTO dto = InsertResultDTO.builder()
                .artifactId("artifact-123")
                .fileName("watermarked.png")
                .s3WatermarkedKey("https://s3.example/watermarks/watermarked.png")
                .message("abcd")
                .sha256("sha")
                .normalizedSha256("nsha")
                .phash(123L)
                .createdAt(LocalDateTime.now())
                .taskId("task-1")
                .build();

        when(watermarkService.insert(eq(userId), any(), eq("abcd"), eq("task-1")))
                .thenReturn(dto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "input.png", "image/png", "pngbytes".getBytes()
        );

        MockMultipartFile message = new MockMultipartFile(
                "message", "", "text/plain", "abcd".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/watermark")
                        .file(file)
                        .file(message)
                        .param("taskId", "task-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // ResponseDTO 기본 필드 체크
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("워터마크 삽입 성공"))
                // data 필드 체크
                .andExpect(jsonPath("$.data.artifactId").value("artifact-123"))
                .andExpect(jsonPath("$.data.fileName").value("watermarked.png"))
                .andExpect(jsonPath("$.data.s3WatermarkedKey").value("https://s3.example/watermarks/watermarked.png"))
                .andExpect(jsonPath("$.data.message").value("abcd"))
                .andExpect(jsonPath("$.data.taskId").value("task-1"));
    }

    @Test
    @DisplayName("GET /api/watermark - 전체 조회 성공 (페이징)")
    @WithMockCustomUser(userId = 7L, role = "USER")
    void getAllWatermarks_success() throws Exception {
        Long userId = 7L;

        InsertResultDTO dto1 = InsertResultDTO.builder()
                .artifactId("a1").fileName("w1.png").message("m1").build();
        InsertResultDTO dto2 = InsertResultDTO.builder()
                .artifactId("a2").fileName("w2.png").message("m2").build();

        when(watermarkService.getAllResult(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(dto1, dto2), PageRequest.of(0, 15), 2));

        mockMvc.perform(get("/api/watermark")
                        .param("page", "0")
                        .param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("워터마크 삽입 기록 전체 조회 성공"))
                // PageImpl가 Jackson으로 직렬화되면 content 배열로 들어감
                .andExpect(jsonPath("$.data.content[0].artifactId").value("a1"))
                .andExpect(jsonPath("$.data.content[1].artifactId").value("a2"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/watermark/{id} - 단건 조회 성공")
    @WithMockCustomUser(userId = 7L, role = "USER")
    void getWatermark_success() throws Exception {
        Long userId = 7L;
        Long id = 5L;

        InsertResultDTO dto = InsertResultDTO.builder()
                .artifactId("ax")
                .fileName("one.png")
                .message("hi")
                .build();

        when(watermarkService.getSingleResult(userId, id)).thenReturn(dto);

        mockMvc.perform(get("/api/watermark/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("워터마크 삽입 기록 조회 성공"))
                .andExpect(jsonPath("$.data.artifactId").value("ax"))
                .andExpect(jsonPath("$.data.fileName").value("one.png"));
    }

    @Test
    @DisplayName("DELETE /api/watermark/{id} - 삭제 성공")
    @WithMockCustomUser(userId = 7L, role = "USER")
    void deleteWatermark_success() throws Exception {
        Long userId = 7L;
        Long id = 77L;

        doNothing().when(watermarkService).deleteWatermark(userId, id);

        mockMvc.perform(delete("/api/watermark/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("워터마크 삽입 기록 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}