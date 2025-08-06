package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.watermark.WatermarkDTO;
import com.deeptruth.deeptruth.service.WatermarkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WatermarkController.class)
@AutoConfigureMockMvc(addFilters = false)
public class WatermarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WatermarkService waterMarkService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllWatermarks_ShouldReturnList() throws Exception {
        // given
        Long userId = 1L;
        WatermarkDTO dto1 = new WatermarkDTO(1L, "original1.jpg", "watermarked1.jpg", LocalDateTime.parse("2024-01-01T00:00:00"));
        WatermarkDTO dto2 = new WatermarkDTO(2L, "original2.jpg", "watermarked2.jpg", LocalDateTime.parse("2024-01-02T00:00:00"));

        // Mockito.when(waterMarkService.getAllResult(userId)).thenReturn(List.of(dto1, dto2));

        // when & then
        mockMvc.perform(get("/watermark")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("워터마크 삽입 기록 전체 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].originalFilePath").value("original1.jpg"));
    }

    @Test
    void getWatermark_ShouldReturnOne() throws Exception {
        // given
        Long userId = 1L;
        Long id = 42L;
        WatermarkDTO dto = new WatermarkDTO(id, "original.jpg", "watermarked.jpg", LocalDateTime.parse("2024-01-01T00:00:00"));

        Mockito.when(waterMarkService.getSingleResult(userId, id)).thenReturn(dto);

        // when & then
        mockMvc.perform(get("/watermark/{id}", id)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("워터마크 삽입 기록 조회 성공"))
                .andExpect(jsonPath("$.data.originalFilePath").value("original.jpg"));
    }

    @Test
    void deleteWatermark_ShouldSucceed() throws Exception {
        // given
        Long userId = 1L;
        Long id = 10L;

        // when & then
        mockMvc.perform(delete("/watermark/{id}", id)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("워터마크 삽입 기록 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());

        Mockito.verify(waterMarkService).deleteWatermark(userId, id);
    }
}
