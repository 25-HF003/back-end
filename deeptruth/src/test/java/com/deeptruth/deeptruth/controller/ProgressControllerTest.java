package com.deeptruth.deeptruth.controller;


import com.deeptruth.deeptruth.base.dto.websocket.ProgressDTO;
import com.deeptruth.deeptruth.config.JwtAuthenticationFilter;
import com.deeptruth.deeptruth.config.SecurityConfig;
import com.deeptruth.deeptruth.testsecurity.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.*;


@WebMvcTest(
        controllers = ProgressController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,
                        JwtAuthenticationFilter.class
                })
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("POST /progress → WebSocket topic으로 ProgressDTO 전달 성공")
    @WithMockCustomUser(userId = 7L, role = "USER")
    void receiveProgress_success() throws Exception {
        String json = """
            {
              "userId" : "7L",
              "taskId": "task-123",
              "progress": 50
            }
            """;

        mockMvc.perform(post("/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // SimpMessagingTemplate이 올바른 경로와 객체로 호출되었는지 검증
        ProgressDTO expectedDto = new ProgressDTO("task-123", 50, "7L");
        verify(messagingTemplate).convertAndSendToUser(
                eq("7L"),
                eq("/topic/progress/task-123"),
                Mockito.refEq(expectedDto)
        );
    }
}