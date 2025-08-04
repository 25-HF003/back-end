package com.deeptruth.deeptruth.config;

import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 매 요청마다 SecurityContext 초기화
        SecurityContextHolder.clearContext();

        // 2. Authorization 헤더에서 토큰 추출
        String token = extractTokenFromHeader(request);

        // 3. 토큰이 있고 유효한 경우에만 인증 처리
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                // 4. 토큰에서 사용자 정보 추출
                String loginId = jwtUtil.getLoginIdFromToken(token);

                // 5. DB에서 사용자 정보 조회
                Optional<User> userOptional = userRepository.findByLoginId(loginId);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    // 6. Spring Security 인증 객체 생성
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 7. SecurityContext에 인증 정보 저장
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("JWT 인증 성공: {}", loginId);
                }
            } catch (Exception e) {
                log.error("JWT 인증 실패: {}", e.getMessage());
                // 예외 발생 시에도 SecurityContext 초기화
                SecurityContextHolder.clearContext();
            }
        } else {
            // 토큰이 없거나 유효하지 않을 때 SecurityContext 초기화
            log.debug("유효하지 않은 토큰 또는 토큰 없음");
            SecurityContextHolder.clearContext();
        }

        // 8. 다음 필터로 전달
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }

        return null;
    }

    /**
     * 인증이 필요없는 경로들은 필터를 건너뛰기
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 인증 불필요 경로들
        return path.startsWith("/api/auth/") ||  // 로그인, 회원가입 등
                path.startsWith("/error") ||      // 에러 페이지
                path.startsWith("/favicon.ico");  // 파비콘
    }
}
