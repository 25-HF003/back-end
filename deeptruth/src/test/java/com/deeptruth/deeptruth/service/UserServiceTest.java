package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.Role;
import com.deeptruth.deeptruth.base.OAuth.OAuth2UserInfo;
import com.deeptruth.deeptruth.base.dto.login.LoginRequestDTO;
import com.deeptruth.deeptruth.base.dto.login.LoginResponse;
import com.deeptruth.deeptruth.entity.RefreshToken;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.RefreshTokenRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.deeptruth.deeptruth.base.dto.signup.SignupRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.deeptruth.deeptruth.constants.LoginConstants.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void 소셜로그인_기존_유저는_조회만_한다() {
        // given
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.getEmail()).thenReturn("hong@example.com");
        when(userRepository.findByEmail("hong@example.com"))
                .thenReturn(Optional.of(User.builder().email("hong@example.com").build()));

        // when
        User result = userService.findOrCreateSocialUser(userInfo, "google");
        // User result = userService.findOrCreateSocialUser(userInfo, "naver");
        // User result = userService.findOrCreateSocialUser(userInfo, "kakao");


        // then
        assertThat(result.getEmail()).isEqualTo("hong@example.com");
        verify(userRepository, never()).save(any());

    }


    @Test
    void 소셜로그인_신규_유저는_DB에_저장된다() {
        // given
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.getEmail()).thenReturn("newuser@example.com");
        when(userInfo.getName()).thenReturn("신규유저");

        when(userRepository.findByEmail("newuser@example.com"))
                .thenReturn(Optional.empty());

        // save()가 저장한 User를 그대로 반환해야 result가 null이 아님
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = userService.findOrCreateSocialUser(userInfo, "google");
        // User result = userService.findOrCreateSocialUser(userInfo, "naver");
        // User result = userService.findOrCreateSocialUser(userInfo, "kakao");


        // then
        assertThat(result.getEmail()).isEqualTo("newuser@example.com");
        verify(userRepository).save(any(User.class));

    }

    // 일반 회원가입 테스트
    @Test
    void 회원가입_성공() {
        // given
        SignupRequestDTO request = new SignupRequestDTO(
                "홍길동", "userid123", "Password1!", "Password1!", "tester", "test@example.com"
        );

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByLoginId(request.getLoginId())).thenReturn(false);
        when(userRepository.existsByNickname(request.getNickname())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("암호화된 비밀번호");

        // when & then
        assertDoesNotThrow(() -> userService.signup(request));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 회원가입_실패_비밀번호_불일치() {
        // given
        SignupRequestDTO request = new SignupRequestDTO(
                "홍길동", "userid123", "Password1!", "DifferentPass1!", "tester", "test@example.com"
        );

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.signup(request));
        assertTrue(exception.getMessage().contains(PASSWORD_MISMATCH_MESSAGE));
    }

    @Test
    void 회원가입_실패_아이디중복() {
        SignupRequestDTO request = new SignupRequestDTO(
                "홍길동", "userid123", "Password1!", "Password1!", "tester2", "test2@example.com"
        );
        when(userRepository.existsByLoginId(request.getLoginId())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.signup(request));
        assertTrue(exception.getMessage().contains("아이디"));
    }

    @Test
    void 회원가입_실패_닉네임중복() {
        SignupRequestDTO request = new SignupRequestDTO(
                "홍길동", "userid124", "Password1!", "Password1!", "tester", "test3@example.com"
        );
        when(userRepository.existsByNickname(request.getNickname())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.signup(request));
        assertTrue(exception.getMessage().contains("닉네임"));
    }

    @Test
    void 회원가입_실패_이메일중복() {
        // given
        SignupRequestDTO request = new SignupRequestDTO(
                "홍길동", "userid125", "Password1!", "Password1!", "tester5", "test@example.com"
        );
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.signup(request));
        assertTrue(exception.getMessage().contains("이메일"));
    }

    @Nested
    @DisplayName("회원가입 실패 - 유효성 검사")
    class InvalidSignupTests {

        @Test
        void 아이디_너무짧거나_너무김() {
            SignupRequestDTO request1 = new SignupRequestDTO("홍길동", "a1", "Password1!", "Password1!", "닉네임", "user1@example.com"); // 너무 짧음
            SignupRequestDTO request2 = new SignupRequestDTO("홍길동", "a12345678901234567890x", "Password1!", "Password1!", "닉네임", "user2@example.com"); // 너무 김
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request1));
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request2));
        }

        @Test
        void 아이디_영소문자_숫자_외_문자_포함() {
            SignupRequestDTO request = new SignupRequestDTO("홍길동", "User_123", "Password1!", "Password1!", "닉네임", "user@example.com"); // 대문자, 언더스코어
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request));
        }

        @Test
        void 아이디_공백포함() {
            SignupRequestDTO request = new SignupRequestDTO("홍길동", "user id", "Password1!", "Password1!", "닉네임", "user@example.com");
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request));
        }

        @Test
        void 비밀번호_아이디와_동일() {
            SignupRequestDTO request = new SignupRequestDTO("홍길동", "userid123", "userid123", "userid123", "닉네임", "user@example.com");
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request));
        }

        @Test
        void 비밀번호_너무짧거나_너무김() {
            SignupRequestDTO request1 = new SignupRequestDTO("홍길동", "userid123", "Pw1!", "Pw1!", "닉네임", "user1@example.com"); // 너무 짧음
            SignupRequestDTO request2 = new SignupRequestDTO("홍길동", "userid123", "P".repeat(31) + "1!", "P".repeat(31) + "1!", "닉네임", "user2@example.com"); // 너무 김
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request1));
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request2));
        }

        @Test
        void 비밀번호_영대소문자_숫자_특수문자_조건_불일치() {
            SignupRequestDTO noUpper = new SignupRequestDTO("홍길동", "userid123", "password1!", "password1!", "닉네임", "user1@example.com");
            SignupRequestDTO noLower = new SignupRequestDTO("홍길동", "userid123", "PASSWORD1!", "PASSWORD1!", "닉네임", "user2@example.com");
            SignupRequestDTO noNumber = new SignupRequestDTO("홍길동", "userid123", "Password!", "Password!", "닉네임", "user3@example.com");
            SignupRequestDTO noSpecial = new SignupRequestDTO("홍길동", "userid123", "Password1", "Password1", "닉네임", "user4@example.com");

            assertThrows(IllegalArgumentException.class, () -> userService.signup(noUpper));
            assertThrows(IllegalArgumentException.class, () -> userService.signup(noLower));
            assertThrows(IllegalArgumentException.class, () -> userService.signup(noNumber));
            assertThrows(IllegalArgumentException.class, () -> userService.signup(noSpecial));
        }

        @Test
        void 닉네임_너무짧거나_너무김() {
            SignupRequestDTO request1 = new SignupRequestDTO("홍길동", "userid123", "Password1!", "Password1!", "닉", "user1@example.com");
            SignupRequestDTO request2 = new SignupRequestDTO("홍길동", "userid123", "Password1!", "Password1!", "닉".repeat(16), "user2@example.com");
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request1));
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request2));
        }

        @Test
        void 닉네임_공백_혹은_특수문자포함() {
            SignupRequestDTO request1 = new SignupRequestDTO("홍길동", "userid123", "Password1!", "Password1!", "닉 네임", "user1@example.com");
            SignupRequestDTO request2 = new SignupRequestDTO("홍길동", "userid123", "Password1!", "Password1!", "닉@네임", "user2@example.com");
            SignupRequestDTO request3 = new SignupRequestDTO("홍길동", "userid123", "Password1!", "Password1!", " nick", "user3@example.com");
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request1));
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request2));
            assertThrows(IllegalArgumentException.class, () -> userService.signup(request3));
        }
    }

    // 로그인 테스트
    /*
    @Test
    void 로그인_성공_JWT토큰_반환() {
        // given
        String loginId = "testuser123";
        String password = "Password1!";
        String encodedPassword = "encoded_password";

        User mockUser = User.builder()
                .userId(1L)
                .loginId(loginId)
                .password(encodedPassword)
                .role(Role.USER)
                .build();

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);
        when(jwtUtil.generateToken(any(User.class)))
                .thenReturn("mocked.jwt.token");

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setLoginId(loginId);
        loginRequest.setPassword(password);

        // when
        String jwtToken = userService.login(loginRequest);

        // then
        assertThat(jwtToken)
                .isNotNull()
                .isNotEmpty()
                .contains(".");

        verify(userRepository).findByLoginId(loginId);
        verify(passwordEncoder).matches(password, encodedPassword);
        verify(jwtUtil).generateToken(any(User.class));
    }
     */

    @Test
    void 로그인_성공시_두_토큰_반환() {
        // given
        LoginRequestDTO request = new LoginRequestDTO("testuser123", "TestPassword1!");

        User mockUser = User.builder()
                .userId(1L)
                .loginId("testuser123")
                .password("encodedPassword")
                .email("test@example.com")
                .nickname("테스트닉네임")
                .name("테스트사용자")
                .role(Role.USER)
                .build();

        when(userRepository.findByLoginId("testuser123")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("TestPassword1!", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken(mockUser)).thenReturn("accessToken123");
        when(jwtUtil.generateRefreshToken(mockUser)).thenReturn("refreshToken456");

        // when
        LoginResponse response = userService.login(request);

        // then
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken123");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken456");

        // verify
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }


    @Test
    void 로그인_실패_존재하지않는_아이디() {
        // given
        String loginId = "nonexistent";

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.empty());

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setLoginId(loginId);
        loginRequest.setPassword("Password1!");

        // when & then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(USER_NOT_FOUND_MESSAGE);

        verify(userRepository).findByLoginId(loginId);
    }

    @Test
    void 로그인_실패_잘못된_비밀번호() {
        // given
        String loginId = "testuser123";
        String password = "wrongpassword";
        String encodedPassword = "encoded_password";

        User mockUser = User.builder()
                .userId(1L)
                .loginId(loginId)
                .password(encodedPassword)
                .build();

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(false);

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setLoginId(loginId);
        loginRequest.setPassword(password);

        // when & then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다.");

        verify(userRepository).findByLoginId(loginId);
        verify(passwordEncoder).matches(password, encodedPassword);
    }
}
