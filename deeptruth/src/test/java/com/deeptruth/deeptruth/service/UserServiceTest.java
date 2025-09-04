package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.Role;
import com.deeptruth.deeptruth.base.Enum.SocialLoginType;
import com.deeptruth.deeptruth.base.OAuth.OAuth2UserInfo;
import com.deeptruth.deeptruth.base.dto.login.LoginRequestDTO;
import com.deeptruth.deeptruth.base.dto.login.LoginResponse;
import com.deeptruth.deeptruth.base.dto.user.UserUpdateRequest;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.RefreshToken;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.RefreshTokenRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.deeptruth.deeptruth.base.dto.signup.SignupRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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

    private User testUser;

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

    // 회원 수정 테스트

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .name("홍길동")
                .loginId("testuser123")
                .password("encodedPassword")
                .nickname("기존닉네임")
                .email("test@example.com")
                .role(Role.USER)
                .socialLoginType(SocialLoginType.NONE)
                .build();
    }

    @Test
    void 회원수정_성공_닉네임변경() {
        // given
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("새로운닉네임");

        when(userRepository.existsByNickname("새로운닉네임")).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);

        // when
        assertDoesNotThrow(() -> userService.updateUser(testUser, request));

        // then
        assertEquals("새로운닉네임", testUser.getNickname());
        verify(userRepository).existsByNickname("새로운닉네임");
        verify(userRepository).save(testUser);
    }

    @Test
    void 회원수정_실패_중복닉네임() {
        // given
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("중복된닉네임");

        when(userRepository.existsByNickname("중복된닉네임")).thenReturn(true);

        // when & then
        DuplicateNicknameException exception = assertThrows(
                DuplicateNicknameException.class,
                () -> userService.updateUser(testUser, request)
        );

        assertEquals("이미 사용 중인 닉네임입니다.", exception.getMessage());
        verify(userRepository).existsByNickname("중복된닉네임");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 회원수정_성공_이메일변경() {
        // given
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("newemail@example.com");

        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);

        // when
        assertDoesNotThrow(() -> userService.updateUser(testUser, request));

        // then
        assertEquals("newemail@example.com", testUser.getEmail());
        verify(userRepository).existsByEmail("newemail@example.com");
        verify(userRepository).save(testUser);
    }

    @Test
    void 회원수정_실패_중복이메일() {
        // given
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("duplicate@example.com");

        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        // when & then
        DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> userService.updateUser(testUser, request)
        );

        assertEquals("이미 사용 중인 이메일입니다.", exception.getMessage());
        verify(userRepository).existsByEmail("duplicate@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 회원수정_실패_소셜로그인사용자_이메일변경시도() {
        // given
        testUser.setSocialLoginType(SocialLoginType.GOOGLE); // 소셜 로그인 사용자로 설정

        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("newemail@example.com");

        // when & then
        UnauthorizedOperationException exception = assertThrows(
                UnauthorizedOperationException.class,
                () -> userService.updateUser(testUser, request)
        );

        assertEquals("소셜 로그인 사용자는 이메일을 변경할 수 없습니다.", exception.getMessage());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 회원수정_성공_비밀번호변경() {
        // given
        UserUpdateRequest request = new UserUpdateRequest();
        request.setCurrentPassword("Password1!");
        request.setNewPassword("NewPassword123!");
        request.setNewPasswordConfirm("NewPassword123!");

        when(passwordEncoder.matches("Password1!", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("newEncodedPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);

        // when
        assertDoesNotThrow(() -> userService.updateUser(testUser, request));

        // then
        assertEquals("newEncodedPassword", testUser.getPassword());
        verify(passwordEncoder).matches("Password1!", "encodedPassword");
        verify(passwordEncoder).encode("NewPassword123!");
        verify(userRepository).save(testUser);
    }

    @Test
    void 회원수정_실패_현재비밀번호불일치() {
        // given
        UserUpdateRequest request = new UserUpdateRequest();
        request.setCurrentPassword("WrongPassword!");
        request.setNewPassword("NewPassword123!");
        request.setNewPasswordConfirm("NewPassword123!");

        when(passwordEncoder.matches("WrongPassword!", "encodedPassword")).thenReturn(false);

        // when & then
        InvalidPasswordException exception = assertThrows(
                InvalidPasswordException.class,
                () -> userService.updateUser(testUser, request)
        );

        assertEquals("현재 비밀번호가 일치하지 않습니다.", exception.getMessage());
        verify(passwordEncoder).matches("WrongPassword!", "encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 회원수정_실패_새비밀번호불일치() {
        // given
        UserUpdateRequest request = new UserUpdateRequest();
        request.setCurrentPassword("Password1!");
        request.setNewPassword("NewPassword123!");
        request.setNewPasswordConfirm("DifferentPassword!");

        when(passwordEncoder.matches("Password1!", "encodedPassword")).thenReturn(true);

        // when & then
        InvalidPasswordException exception = assertThrows(
                InvalidPasswordException.class,
                () -> userService.updateUser(testUser, request)
        );

        assertEquals("새 비밀번호 확인이 일치하지 않습니다.", exception.getMessage());
        verify(passwordEncoder).matches("Password1!", "encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 회원수정_실패_새비밀번호가_현재비밀번호와_동일() {
        // given
        UserUpdateRequest request = new UserUpdateRequest();
        request.setCurrentPassword("Password1!");
        request.setNewPassword("Password1!");
        request.setNewPasswordConfirm("Password1!");

        // 현재 비밀번호 검증은 성공하도록 설정
        when(passwordEncoder.matches("Password1!", "encodedPassword")).thenReturn(true);

        // when & then
        InvalidPasswordException exception = assertThrows(
                InvalidPasswordException.class,
                () -> userService.updateUser(testUser, request)
        );

        assertEquals("새 비밀번호는 현재 비밀번호와 달라야 합니다.", exception.getMessage());

        verify(passwordEncoder).matches("Password1!", "encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void 회원수정_실패_소셜로그인사용자_비밀번호변경시도() {
        // given
        testUser.setSocialLoginType(SocialLoginType.GOOGLE); // 소셜 로그인 사용자로 설정

        UserUpdateRequest request = new UserUpdateRequest();
        request.setCurrentPassword("Password1!");
        request.setNewPassword("NewPassword123!");
        request.setNewPasswordConfirm("NewPassword123!");

        // when & then
        UnauthorizedOperationException exception = assertThrows(
                UnauthorizedOperationException.class,
                () -> userService.updateUser(testUser, request)
        );

        assertEquals("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // 회원 탈퇴 테스트

    @Test
    void 회원탈퇴_성공_SoftDelete() {
        // given
        User user = User.builder()
                .userId(1L)
                .loginId("testuser")
                .email("test@example.com")
                .deletedAt(null)  // 초기에는 삭제되지 않은 상태
                .build();

        // when
        assertDoesNotThrow(() -> userService.deleteUser(user));

        // then - Repository 호출 확인만 (실제 deletedAt 설정은 @SQLDelete가 처리)
        verify(refreshTokenRepository).deleteByUser(user);
        verify(userRepository).delete(user);  // 이것이 실제로는 UPDATE 쿼리가 됨
    }

    @Test
    void 삭제된_사용자는_조회되지_않음() {
        // given
        String loginId = "deleteduser";
        when(userRepository.findByLoginId(loginId)).thenReturn(Optional.empty());

        // when
        Optional<User> result = userRepository.findByLoginId(loginId);

        // then
        assertTrue(result.isEmpty());
        verify(userRepository).findByLoginId(loginId);
    }

    @Test
    void 활성_사용자만_조회() {
        // given - @SQLRestriction으로 자동 필터링되는 활성 사용자들만 목킹
        List<User> activeUsers = Arrays.asList(
                User.builder().userId(1L).loginId("user1").deletedAt(null).build(),
                User.builder().userId(2L).loginId("user2").deletedAt(null).build()
        );

        when(userRepository.findAll()).thenReturn(activeUsers);

        // when
        List<User> result = userRepository.findAll();

        // then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(user -> user.getDeletedAt() == null));
    }


    // 회원 조회 테스트

    @Test
    void 회원조회_성공() {
        // given
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

        // when
        User result = userService.findUserById(1L);

        // then
        assertNotNull(result);
        assertEquals(testUser.getUserId(), result.getUserId());
        assertEquals(testUser.getLoginId(), result.getLoginId());
        assertEquals(testUser.getNickname(), result.getNickname());
        verify(userRepository).findById(1L);
    }

    @Test
    void 회원조회_실패_사용자없음() {
        // given
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.findUserById(999L)
        );

        // UserNotFoundException의 메시지는 생성자에 따라 다를 수 있음
        verify(userRepository).findById(999L);
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
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

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
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다.");

        verify(userRepository).findByLoginId(loginId);
        verify(passwordEncoder).matches(password, encodedPassword);
    }
}
