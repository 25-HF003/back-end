package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.Role;
import com.deeptruth.deeptruth.base.Enum.SocialLoginType;
import com.deeptruth.deeptruth.base.OAuth.OAuth2UserInfo;
import com.deeptruth.deeptruth.base.dto.login.LoginRequestDTO;
import com.deeptruth.deeptruth.base.dto.login.LoginResponse;
import com.deeptruth.deeptruth.base.dto.signup.SignupRequestDTO;
import com.deeptruth.deeptruth.base.dto.user.UserUpdateRequest;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import com.deeptruth.deeptruth.repository.RefreshTokenRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.deeptruth.deeptruth.entity.RefreshToken;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import java.util.UUID;

import static com.deeptruth.deeptruth.constants.LoginConstants.*;
import static com.deeptruth.deeptruth.constants.SignupConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public User findOrCreateSocialUser(OAuth2UserInfo oAuth2UserInfo, String provider){

        return userRepository.findByEmail(oAuth2UserInfo.getEmail())
                .orElseGet(() ->
                    userRepository.save(User.builder()
                            .email(oAuth2UserInfo.getEmail())
                            .name(oAuth2UserInfo.getName())
                            .loginId(provider + "_" + UUID.randomUUID())
                            .nickname(generateUniqueNickname(oAuth2UserInfo.getName()))
                            .password("NO_PASSWORD")
                            .role(Role.USER)
                            .socialLoginType(SocialLoginType.valueOf(provider.toUpperCase()))
                            .build()
                ));
    }

    private String generateUniqueNickname(String baseName){
        String nickname = baseName;
        int count=1;
        while(userRepository.existsByNickname(nickname)){
            nickname = baseName + "_" + count;
            count++;
        }
        return nickname;
    }

    // 일반 회원가입
    @Transactional
    public void signup(SignupRequestDTO request) {
        // 유효성 검사
        validateLoginId(request.getLoginId());
        validatePassword(request.getPassword(), request.getPasswordConfirm(), request.getLoginId());
        validateNickname(request.getNickname());

        // 중복 검사
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new DuplicateLoginIdException(DUPLICATE_LOGIN_ID_MESSAGE);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(DUPLICATE_EMAIL_MESSAGE);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new DuplicateNicknameException(DUPLICATE_NICKNAME_MESSAGE);
        }

        // 비밀번호 암호화
        String encodedPwd = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .name(request.getName())
                .loginId(request.getLoginId())
                .password(encodedPwd)
                .nickname(request.getNickname())
                .email(request.getEmail())
                .role(Role.USER)
                .build();

        userRepository.save(user);
    }

    private void validateLoginId(String loginId) {
        if (!loginId.matches(LOGIN_ID_PATTERN)) {
            throw new IllegalArgumentException(LOGIN_ID_ERROR_MESSAGE);
        }
    }

    private void validatePassword(String pw, String pwConfirm, String loginId) {
        if (!pw.equals(pwConfirm)) {
            throw new IllegalArgumentException(PASSWORD_MISMATCH_MESSAGE);
        }
        if (pw.equals(loginId)) {
            throw new IllegalArgumentException("비밀번호는 아이디와 동일할 수 없습니다.");
        }
        if (!pw.matches(PASSWORD_PATTERN)) {
            throw new IllegalArgumentException(PASSWORD_ERROR_MESSAGE);
        }
    }

    private void validateNickname(String nickname) {
        if (!nickname.matches(NICKNAME_PATTERN)) {
            throw new IllegalArgumentException(NICKNAME_ERROR_MESSAGE);
        }
    }

    // 로그인
    public LoginResponse login(LoginRequestDTO loginRequestDTO) {
        // null 체크
        if (loginRequestDTO.getLoginId() == null || loginRequestDTO.getLoginId().trim().isEmpty()) {
            throw new IllegalArgumentException(EMPTY_LOGIN_ID_MESSAGE);
        }

        if (loginRequestDTO.getPassword() == null || loginRequestDTO.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException(EMPTY_PASSWORD_MESSAGE);
        }

        // 사용자 조회
        User user = userRepository.findByLoginId(loginRequestDTO.getLoginId())
                .orElseThrow(() -> new UserNotFoundException(null));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException(PASSWORD_MISMATCH_MESSAGE);
        }

        // 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user);   // 1시간
        String refreshToken = jwtUtil.generateRefreshToken(user); // 2주

        // Refresh Token DB 저장
        saveOrUpdateRefreshToken(user, refreshToken);

        // LoginResponse 객체로 반환
        return new LoginResponse(accessToken, refreshToken);
    }

    // Refresh Token 저장/업데이트 메서드
    private void saveOrUpdateRefreshToken(User user, String refreshToken) {
        // 기존 Refresh Token이 있는지 확인
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        if (existingToken.isPresent()) {
            // 기존 토큰이 있으면 업데이트 (중복 방지)
            RefreshToken token = existingToken.get();
            token.setToken(refreshToken);
            refreshTokenRepository.save(token);
        } else {
            // 신규 사용자면 새로 생성
            RefreshToken newToken = RefreshToken.builder()
                    .user(user)
                    .token(refreshToken)
                    .build();
            refreshTokenRepository.save(newToken);
        }
    }

    // 토큰 재발급 메서드
    public LoginResponse refreshAccessToken(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. DB에서 Refresh Token 확인
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Refresh Token입니다."));

        // 3. 만료 여부 확인
        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken); // 만료된 토큰 삭제
            throw new IllegalArgumentException("만료된 Refresh Token입니다.");
        }

        // 4. 사용자 정보로 새로운 Access Token 생성
        User user = storedToken.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user);

        // 5. 새로운 Refresh Token도 생성 (보안 강화)
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        // 6. DB의 Refresh Token 업데이트
        storedToken.setToken(newRefreshToken);
        refreshTokenRepository.save(storedToken);

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh Token이 필요합니다.");
        }

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        Optional<RefreshToken> storedToken = refreshTokenRepository.findByToken(refreshToken);

        if (storedToken.isPresent()) {
            refreshTokenRepository.delete(storedToken.get());
        } else {
            throw new IllegalArgumentException("이미 로그아웃되었거나 존재하지 않는 Refresh Token입니다.");
        }
    }

    // 회원정보 수정
    @Transactional
    public void updateUser(User user, UserUpdateRequest request) {
        // 닉네임 변경 처리
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            validateNickname(request.getNickname());
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new DuplicateNicknameException("이미 사용 중인 닉네임입니다.");
            }
            user.setNickname(request.getNickname());
        }

        // 이메일 변경 처리 (일반)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (user.getSocialLoginType() != SocialLoginType.NONE) {
                throw new UnauthorizedOperationException("소셜 로그인 사용자는 이메일을 변경할 수 없습니다.");
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
            }
            user.setEmail(request.getEmail());
        }

        // 비밀번호 변경 처리 (일반)
        if (request.getNewPassword() != null) {
            if (user.getSocialLoginType() != SocialLoginType.NONE) {
                throw new UnauthorizedOperationException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
            }

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new InvalidPasswordException("현재 비밀번호가 일치하지 않습니다.");
            }

            if (request.getNewPassword().equals(request.getCurrentPassword())) {
                throw new InvalidPasswordException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
            }

            if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
                throw new InvalidPasswordException("새 비밀번호 확인이 일치하지 않습니다.");
            }

            validatePassword(request.getNewPassword(), request.getNewPasswordConfirm(), user.getLoginId());
            String encoded = passwordEncoder.encode(request.getNewPassword());
            user.setPassword(encoded);
        }

        userRepository.save(user);
    }

    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    // 회원 탈퇴
    @Transactional
    public void deleteUser(User user) {
        // RefreshToken만 실제 삭제 - 보안상 필요
        refreshTokenRepository.deleteByUser(user);

        // User는 Soft Delete - 자동으로 UPDATE 쿼리 실행
        userRepository.delete(user);  // deleted = true로 변경

        log.info("사용자 소프트 삭제 완료: userId={}", user.getUserId());
    }


}
