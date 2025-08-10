package com.deeptruth.deeptruth.config;

import com.deeptruth.deeptruth.base.OAuth.GoogleUserDetails;
import com.deeptruth.deeptruth.base.OAuth.KakaoUserDetails;
import com.deeptruth.deeptruth.base.OAuth.NaverUserDetails;
import com.deeptruth.deeptruth.base.OAuth.OAuth2UserInfo;
import com.deeptruth.deeptruth.entity.RefreshToken;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.RefreshTokenRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public OAuth2AuthenticationSuccessHandler(JwtUtil jwtUtil,
                                              UserRepository userRepository,
                                              RefreshTokenRepository refreshTokenRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String provider = resolveProvider(authentication);
        OAuth2UserInfo userInfo = getOAuth2UserInfo(provider, attributes);

        // 이미 로그인 성공 시점에서 회원가입 또는 조회된 상태
        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

        // JWT 발급
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenStr = jwtUtil.generateRefreshToken(user);

        // RefreshToken 저장 또는 갱신
        refreshTokenRepository.findByUser(user).ifPresentOrElse(
                existing -> {
                    existing.setToken(refreshTokenStr);
                    existing.setCreatedAt(LocalDateTime.now());
                    refreshTokenRepository.save(existing);
                },
                () -> {
                    RefreshToken newToken = RefreshToken.builder()
                            .user(user)
                            .token(refreshTokenStr)
                            .build();
                    refreshTokenRepository.save(newToken);
                }
        );

        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshTokenStr)
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }


    private String resolveProvider(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oAuth2Token) {
            return oAuth2Token.getAuthorizedClientRegistrationId(); // "kakao", "google" 등
        }
        return "unknown"; // fallback
    }

    private OAuth2UserInfo getOAuth2UserInfo(String provider, Map<String, Object> attributes) {
        return switch (provider.toLowerCase()) {
            case "google" -> new GoogleUserDetails(attributes);
            case "kakao" -> new KakaoUserDetails(attributes);
            case "naver" -> new NaverUserDetails(attributes);
            default -> throw new RuntimeException("Unsupported provider: " + provider);
        };
    }
}