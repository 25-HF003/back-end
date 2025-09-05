package com.deeptruth.deeptruth.base.dto.user;

import com.deeptruth.deeptruth.base.Enum.Level;
import com.deeptruth.deeptruth.base.Enum.Role;
import com.deeptruth.deeptruth.base.Enum.SocialLoginType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    private Long userId;
    private String loginId;
    private String name;
    private String nickname;
    private String email;
    private Role role;
    private Level level;           // 사용자 레벨
    private int point;             // 포인트
    private String signature;      // 서명

    // 계정 정보
    private SocialLoginType socialLoginType;  // 소셜로그인 타입
    private LocalDate createdAt;              // 가입일
}
