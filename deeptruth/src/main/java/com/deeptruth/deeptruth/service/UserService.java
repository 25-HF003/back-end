package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.Role;
import com.deeptruth.deeptruth.base.Enum.SocialLoginType;
import com.deeptruth.deeptruth.base.OAuth.OAuth2UserInfo;
import com.deeptruth.deeptruth.base.dto.login.LoginRequestDTO;
import com.deeptruth.deeptruth.base.dto.login.LoginResponse;
import com.deeptruth.deeptruth.base.dto.signup.SignupRequestDTO;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.RefreshTokenRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.deeptruth.deeptruth.entity.RefreshToken;
import com.deeptruth.deeptruth.base.dto.login.LoginResponse;
import java.util.Optional;

import java.util.UUID;

import static com.deeptruth.deeptruth.constants.LoginConstants.*;
import static com.deeptruth.deeptruth.constants.SignupConstants.*;

@Service
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

    public boolean existsByUserId(Long userId){
        return userRepository.existsByUserId(userId);
    }

    // 일반 회원가입
    public void signup(SignupRequestDTO request) {
        // 유효성 검사
        validateLoginId(request.getLoginId());
        validatePassword(request.getPassword(), request.getPasswordConfirm(), request.getLoginId());
        validateNickname(request.getNickname());

        // 중복 검사
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException(DUPLICATE_LOGIN_ID_MESSAGE);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(DUPLICATE_EMAIL_MESSAGE);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException(DUPLICATE_NICKNAME_MESSAGE);
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
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MESSAGE));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException(PASSWORD_MISMATCH_MESSAGE);
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

}
