package com.deeptruth.deeptruth.controller;

import static com.deeptruth.deeptruth.constants.LoginConstants.*;
import static com.deeptruth.deeptruth.constants.SignupConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.deeptruth.deeptruth.base.Enum.Level;
import com.deeptruth.deeptruth.base.Enum.Role;
import com.deeptruth.deeptruth.base.Enum.SocialLoginType;
import com.deeptruth.deeptruth.base.dto.login.LoginRequestDTO;
import com.deeptruth.deeptruth.base.dto.login.LoginResponse;
import com.deeptruth.deeptruth.base.dto.signup.SignupRequestDTO;
import com.deeptruth.deeptruth.base.dto.user.UserUpdateRequest;
import com.deeptruth.deeptruth.config.CustomUserDetails;
import com.deeptruth.deeptruth.config.JwtAuthenticationFilter;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.service.UserService;
import com.deeptruth.deeptruth.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    // SecurityContext 설정 헬퍼 메서드
    private void setUpSecurityContext(Long userId, String loginId) {
        User mockUser = User.builder()
                .userId(userId)
                .loginId(loginId)
                .role(Role.USER)
                .build();

        CustomUserDetails customUserDetails = new CustomUserDetails(mockUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUserDetails, null, customUserDetails.getAuthorities());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void 회원가입_성공() throws Exception {
        // given
        SignupRequestDTO request = new SignupRequestDTO(
                "홍길동", "userid123", "Password1!", "Password1!", "tester", "test@example.com"
        );

        // when & then
        mockMvc.perform(post("/api/auth/signup")
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
    void 로그인_성공() throws Exception {
        // given
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setLoginId("testuser123");
        loginRequest.setPassword("Password1!");

        LoginResponse mockLoginResponse = new LoginResponse(
                "mocked.access.token",
                "mocked.refresh.token"
        );

        when(userService.login(any(LoginRequestDTO.class)))
                .thenReturn(mockLoginResponse);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(LOGIN_SUCCESS_MESSAGE))
                .andExpect(jsonPath("$.data.accessToken").value("mocked.access.token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mocked.refresh.token"));

        verify(userService).login(any(LoginRequestDTO.class));
    }

    @Test
    void 로그인_실패_존재하지않는_아이디() throws Exception {
        // given
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setLoginId("nonexistent");
        loginRequest.setPassword("Password1!");

        when(userService.login(any(LoginRequestDTO.class)))
                .thenThrow(new IllegalArgumentException(USER_NOT_FOUND_MESSAGE));

        // when & then
        mockMvc.perform(post("/api/auth/login")
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
    void 로그인_실패_잘못된_비밀번호() throws Exception {
        // given
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setLoginId("testuser123");
        loginRequest.setPassword("wrongpassword");

        when(userService.login(any(LoginRequestDTO.class)))
                .thenThrow(new IllegalArgumentException(PASSWORD_MISMATCH_MESSAGE));

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .with(jwt().jwt(builder -> builder
                                .claim("sub", "testuser")
                                .claim("scope", "read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(PASSWORD_MISMATCH_MESSAGE))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(userService).login(any(LoginRequestDTO.class));
    }

    @Test
    void 회원정보_수정_성공() throws Exception {
        // given
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("새닉네임");

        User mockUser = User.builder()
                .userId(1L)
                .loginId("testuser123")
                .nickname("기존닉네임")
                .role(Role.USER)
                .build();

        // SecurityContext 설정
        setUpSecurityContext(1L, "testuser123");

        // UserService 모킹
        when(userService.findUserById(1L)).thenReturn(mockUser);
        doNothing().when(userService).updateUser(any(User.class), any(UserUpdateRequest.class));

        // when & then
        mockMvc.perform(put("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원정보가 성공적으로 변경되었습니다."));

        verify(userService).findUserById(1L);
        verify(userService).updateUser(any(User.class), any(UserUpdateRequest.class));
    }

    @Test
    void 회원탈퇴_성공() throws Exception {
        // given
        User mockUser = User.builder()
                .userId(1L)
                .loginId("testuser123")
                .role(Role.USER)
                .build();

        setUpSecurityContext(1L, "testuser123");

        when(userService.findUserById(1L)).thenReturn(mockUser);
        doNothing().when(userService).deleteUser(any(User.class));

        // when & then
        mockMvc.perform(delete("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));

        verify(userService).findUserById(1L);
        verify(userService).deleteUser(any(User.class));
    }

    @Test
    void 회원_프로필_조회_성공() throws Exception {
        // given
        User mockUser = User.builder()
                .userId(1L)
                .loginId("testuser123")
                .name("홍길동")
                .nickname("테스터")
                .email("test@example.com")
                .role(Role.USER)
                .level(Level.EXPLORER)
                .point(100)
                .signature("안녕하세요")
                .socialLoginType(SocialLoginType.NONE)
                .createdAt(LocalDate.now())
                .build();

        CustomUserDetails customUserDetails = new CustomUserDetails(mockUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUserDetails, null, customUserDetails.getAuthorities());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when & then
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.loginId").value("testuser123"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.nickname").value("테스터"))
                .andExpect(jsonPath("$.data.level").value("EXPLORER"))
                .andExpect(jsonPath("$.data.point").value(100));
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
