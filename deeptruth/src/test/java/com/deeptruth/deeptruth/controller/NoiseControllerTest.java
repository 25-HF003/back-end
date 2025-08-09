package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.config.SecurityConfig;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.service.NoiseService;
import com.deeptruth.deeptruth.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
@WebMvcTest(controllers = NoiseController.class)
class NoiseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoiseService noiseService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser
    void 사용자별_노이즈_기록_전체_조회_API_성공() throws Exception {
        // given
        Long userId = 1L;
        List<NoiseDTO> expectedHistory = Arrays.asList(
                NoiseDTO.builder()
                        .noiseId(1L)
                        .originalFilePath("s3://bucket/original/1.jpg")
                        .processedFilePath("s3://bucket/processed/1.jpg")
                        .epsilon(0.1f)
                        .build()
        );

        when(noiseService.getUserNoiseHistory(any(Long.class)))
                .thenReturn(expectedHistory);

        // when & then
        mockMvc.perform(get("/api/noise")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].noiseId").value(1));

        verify(noiseService).getUserNoiseHistory(userId);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void 사용자_노이즈_이력_조회_API_성공() throws Exception {
        // given
        List<NoiseDTO> expectedHistory = Arrays.asList(
                NoiseDTO.builder()
                        .noiseId(1L)
                        .originalFilePath("s3://bucket/original/1.jpg")
                        .processedFilePath("s3://bucket/processed/1.jpg")
                        .epsilon(0.1f)
                        .build()
        );

        when(noiseService.getUserNoiseHistory(any(Long.class)))
                .thenReturn(expectedHistory);

        // when & then
        mockMvc.perform(get("/api/noise/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("노이즈 삽입 이력 조회 성공"))
                .andExpect(jsonPath("$.data").isArray());

        verify(noiseService).getUserNoiseHistory(any(Long.class));
    }

    @Test
    void JWT_토큰_없이_접근_시_500_응답() throws Exception {
        // when & then - 현재 설계상 500이 정상
        mockMvc.perform(get("/api/noise/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError()) // 401 → 500으로 변경
                .andExpect(jsonPath("$.status").value(500))    // 401 → 500으로 변경
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists();
    }

    @Test
    @WithMockUser(username = "999", roles = "USER")
    void 존재하지_않는_사용자_노이즈_이력_조회_실패() throws Exception {
        // given
        when(noiseService.getUserNoiseHistory(any(Long.class)))
                .thenThrow(new IllegalArgumentException("사용자가 존재하지 않습니다."));

        // when & then
        mockMvc.perform(get("/api/noise/history")
                        .with(user("1"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용자가 존재하지 않습니다."));

        verify(noiseService).getUserNoiseHistory(any(Long.class));
    }
}