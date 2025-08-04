package com.deeptruth.deeptruth.repository;

import com.deeptruth.deeptruth.entity.RefreshToken;
import com.deeptruth.deeptruth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 사용자로 토큰 찾기 (로그인 시 기존 토큰 확인)
    Optional<RefreshToken> findByUser(User user);

    // 토큰 문자열로 찾기 (재발급 시 검증)
    Optional<RefreshToken> findByToken(String token);

    // 사용자별 토큰 삭제 (로그아웃 시 사용)
    void deleteByUser(User user);
}
