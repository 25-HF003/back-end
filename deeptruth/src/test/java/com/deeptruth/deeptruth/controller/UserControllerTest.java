package com.deeptruth.deeptruth.controller;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("소셜 로그인한 유저의 정보를 조회한다")
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void getCurrentSocialUser_success() throws Exception {
        // given
        User user = User.builder()
                .userId(1L)
                .name("테스트유저")
                .loginId("testuser")
                .password("암호화된비밀번호")
                .nickname("닉네임")
                .email("testuser@example.com")
                .role(com.deeptruth.deeptruth.base.Enum.Role.USER)
                .createdAt(LocalDate.of(2025, 5, 18))
                .build();
        Mockito.when(userService.getCurrentUser()).thenReturn(user);

        // when & then
        mockMvc.perform(get("/api/user/me")
                        .with(oauth2Login().attributes(attrs -> {
                            attrs.put("email", "test@example.com");
                            attrs.put("name", "테스트유저");
                        })))
                .andExpect(status().isOk())  // ✅ ResultMatcher
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트유저"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 요청하면 401 Unauthorized를 반환한다")
    void getCurrentUser_unauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }
}
