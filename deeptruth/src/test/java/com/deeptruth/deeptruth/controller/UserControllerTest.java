package com.deeptruth.deeptruth.controller;

import static com.deeptruth.deeptruth.constants.LoginConstants.*;
import static com.deeptruth.deeptruth.constants.SignupConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.deeptruth.deeptruth.base.dto.login.LoginRequestDTO;
import com.deeptruth.deeptruth.base.dto.signup.SignupRequestDTO;
import com.deeptruth.deeptruth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 API 성공 테스트")
    void signupApiSuccess() throws Exception {
        // given
        SignupRequestDTO request = new SignupRequestDTO(
                "홍길동", "userid123", "Password1!", "Password1!", "tester", "test@example.com"
        );

        // when & then
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(SIGNUP_SUCCESS_MESSAGE))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(userService).signup(any(SignupRequestDTO.class));
    }

    @Test
    @DisplayName("로그인 API 성공 테스트")
    void loginApiSuccess() throws Exception {
        // given
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setLoginId("testuser123");
        loginRequest.setPassword("Password1!");

        when(userService.login(any(LoginRequestDTO.class)))
                .thenReturn("mocked.jwt.token");

        // when & then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(LOGIN_SUCCESS_MESSAGE))
                .andExpect(jsonPath("$.data").value("mocked.jwt.token"));

        verify(userService).login(any(LoginRequestDTO.class));
    }

    @Test
    @DisplayName("로그인 API 실패 테스트 - 존재하지 않는 아이디")
    void loginApiFailure_UserNotFound() throws Exception {
        // given
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setLoginId("nonexistent");
        loginRequest.setPassword("Password1!");

        when(userService.login(any(LoginRequestDTO.class)))
                .thenThrow(new IllegalArgumentException(USER_NOT_FOUND_MESSAGE));

        // when & then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(USER_NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(userService).login(any(LoginRequestDTO.class));
    }

    @Test
    @DisplayName("로그인 API 실패 테스트 - 잘못된 비밀번호")
    void loginApiFailure_InvalidPassword() throws Exception {
        // given
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setLoginId("testuser123");
        loginRequest.setPassword("wrongpassword");

        when(userService.login(any(LoginRequestDTO.class)))
                .thenThrow(new IllegalArgumentException(PASSWORD_MISMATCH_MESSAGE));

        // when & then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(PASSWORD_MISMATCH_MESSAGE))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(userService).login(any(LoginRequestDTO.class));
    }

}


/*
@WebMvcTest(UserController.class)
public class UserControllerTest {
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private UserService userService;
//
//    @Test
//    @DisplayName("소셜 로그인한 유저의 정보를 조회한다")
//    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
//    void getCurrentSocialUser_success() throws Exception {
//        // given
//        User user = User.builder()
//                .userId(1L)
//                .name("테스트유저")
//                .loginId("testuser")
//                .password("암호화된비밀번호")
//                .nickname("닉네임")
//                .email("testuser@example.com")
//                .role(com.deeptruth.deeptruth.base.Enum.Role.USER)
//                .createdAt(LocalDate.of(2025, 5, 18))
//                .build();
//        Mockito.when(userService.getCurrentUser()).thenReturn(user);
//
//        // when & then
//        mockMvc.perform(get("/api/user/me")
//                        .with(oauth2Login().attributes(attrs -> {
//                            attrs.put("email", "test@example.com");
//                            attrs.put("name", "테스트유저");
//                        })))
//                .andExpect(status().isOk())  // ✅ ResultMatcher
//                .andExpect(jsonPath("$.email").value("test@example.com"))
//                .andExpect(jsonPath("$.name").value("테스트유저"));
//    }
//
//    @Test
//    @DisplayName("인증되지 않은 사용자가 요청하면 401 Unauthorized를 반환한다")
//    void getCurrentUser_unauthorized() throws Exception {
//        mockMvc.perform(get("/api/user/me"))
//                .andExpect(status().isUnauthorized());
//    }
}
 */
