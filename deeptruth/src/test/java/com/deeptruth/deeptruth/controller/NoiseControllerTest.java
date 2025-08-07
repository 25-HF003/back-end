package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.config.SecurityConfig;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.service.NoiseService;
import com.deeptruth.deeptruth.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NoiseController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
        })
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
    void 사용자별_노이즈_기록_전체_조회() throws Exception {
        // given
        Long userId = 1L;

        // when & then
        mockMvc.perform(get("/api/noise")
                        .param("userId", String.valueOf(userId)))
                .andDo(print())
                .andExpect(status().isOk());
    }
}