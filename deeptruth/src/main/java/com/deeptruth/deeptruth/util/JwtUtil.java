package com.deeptruth.deeptruth.util;

import com.deeptruth.deeptruth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.deeptruth.deeptruth.constants.LoginConstants.*;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessToken = 60 * 60 * 1000L;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshToken = 14 * 24 * 60 * 60 * 1000L;

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getLoginId())
                .claim(USER_ID_CLAIM, user.getUserId())
                .claim(ROLE_CLAIM, user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessToken))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getLoginId())
                .claim(USER_ID_CLAIM, user.getUserId())
                // Role 제외 (보안상 최소 정보만)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshToken))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();
            return String.valueOf(claims.get(USER_ID_CLAIM));
        } catch (JwtException e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String getLoginIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    // 기존 메서드
    @Deprecated
    public String generateToken(User user) {
        return generateAccessToken(user); // 내부적으로 새 메서드 호출
    }
}

