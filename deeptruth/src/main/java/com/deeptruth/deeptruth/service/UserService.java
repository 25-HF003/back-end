package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.Role;
import com.deeptruth.deeptruth.base.Enum.SocialLoginType;
import com.deeptruth.deeptruth.base.OAuth.OAuth2UserInfo;
import com.deeptruth.deeptruth.base.dto.login.LoginRequestDTO;
import com.deeptruth.deeptruth.base.dto.signup.SignupRequestDTO;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 정규식 상수 추출
    private static final String LOGIN_ID_PATTERN = "^[a-z0-9]{6,20}$";
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*\\W).{8,30}$";
    private static final String NICKNAME_PATTERN = "^[가-힣a-zA-Z0-9]{2,15}$";

    // 에러 메시지 상수 추출
    private static final String LOGIN_ID_ERROR_MESSAGE = "아이디는 6~20자의 영소문자 및 숫자만 가능합니다.";
    private static final String PASSWORD_ERROR_MESSAGE = "비밀번호는 8~30자, 영대/소문자·숫자·특수문자를 모두 포함해야 합니다.";
    private static final String NICKNAME_ERROR_MESSAGE = "닉네임은 2~15자, 공백 및 특수문자를 제외한 한글/영문/숫자만 가능합니다.";

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
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
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
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
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

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String login(LoginRequestDTO loginRequestDTO) {
        // 사용자 조회
        User user = userRepository.findByLoginId(loginRequestDTO.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        // JWT 토큰 생성
        return generateJwtToken(user);
    }

    private String generateJwtToken(User user) {
        return Jwts.builder()
                .setSubject(user.getLoginId())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }
}
