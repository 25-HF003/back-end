package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.config.JwtAuthenticationFilter;
import com.deeptruth.deeptruth.config.SecurityConfig;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.service.NoiseService;
import com.deeptruth.deeptruth.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
@DisplayName("NoiseController 통합 테스트")
class NoiseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoiseService noiseService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    /*
    @Test
    @WithMockUser(username = "1", roles = "USER")
    @DisplayName("사용자 노이즈 이력 조회 API 성공")
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

        when(noiseService.getUserNoiseHistory(1L))
                .thenReturn(expectedHistory);

        // when & then
        mockMvc.perform(get("/api/noise/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].noiseId").value(1));

        verify(noiseService).getUserNoiseHistory(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    @DisplayName("노이즈 개별 조회 성공")
    void 노이즈_개별_조회_성공() throws Exception {
        // given - 이 부분이 빠져있었음!
        NoiseDTO mockNoise = NoiseDTO.builder()
                .noiseId(1L)
                .originalFilePath("s3://bucket/original/1.jpg")
                .processedFilePath("s3://bucket/processed/1.jpg")
                .epsilon(0.1f)
                .build();

        when(noiseService.getNoiseById(1L, 1L))
                .thenReturn(mockNoise);

        // when & then
        mockMvc.perform(get("/api/noise/{noiseId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("노이즈 조회 성공"))
                .andExpect(jsonPath("$.data.noiseId").value(1))
                .andExpect(jsonPath("$.data.originalFilePath").exists())
                .andExpect(jsonPath("$.data.processedFilePath").exists());

        verify(noiseService).getNoiseById(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    @DisplayName("존재하지 않는 노이즈 조회 시 404 응답")
    void 존재하지_않는_노이즈_조회_실패() throws Exception {
        // given - Mock 예외 설정
        when(noiseService.getNoiseById(1L, 999L))
                .thenThrow(new IllegalArgumentException("노이즈를 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/noise/{noiseId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("노이즈를 찾을 수 없습니다."));

        verify(noiseService).getNoiseById(1L, 999L);
    }
*/
    @Test
    @DisplayName("JWT 토큰 없이 접근 시 인증 오류")
    void JWT_토큰_없이_접근_시_인증_오류() throws Exception {
        // when & then
        mockMvc.perform(get("/api/noise/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()) // 401이 맞음
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.success").value(false));
    }
}
