package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.login.LoginRequestDTO;
import com.deeptruth.deeptruth.base.dto.login.LoginResponse;
import com.deeptruth.deeptruth.base.dto.login.RefreshTokenRequest;
import com.deeptruth.deeptruth.base.dto.signup.SignupRequestDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.base.dto.user.UserUpdateRequest;
import com.deeptruth.deeptruth.base.exception.DuplicateNicknameException;
import com.deeptruth.deeptruth.config.CustomUserDetails;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.deeptruth.deeptruth.base.dto.user.UserProfile;
import org.springframework.security.core.Authentication;

import static com.deeptruth.deeptruth.constants.LoginConstants.*;
import static com.deeptruth.deeptruth.constants.SignupConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/signup")
    public ResponseEntity<ResponseDTO<Void>> signup(@RequestBody SignupRequestDTO request) {
        userService.signup(request);

        return ResponseEntity.ok(
                ResponseDTO.success(
                        HttpStatus.OK.value(),
                        SIGNUP_SUCCESS_MESSAGE,
                        null
                )
        );
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ResponseDTO<LoginResponse>> login(@RequestBody LoginRequestDTO request) {
        LoginResponse loginResponse = userService.login(request);

        return ResponseEntity.ok(
                ResponseDTO.success(
                        HttpStatus.OK.value(),
                        LOGIN_SUCCESS_MESSAGE,
                        loginResponse
                )
        );
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ResponseDTO<LoginResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = userService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(
                ResponseDTO.success(HttpStatus.OK.value(), "토큰 재발급 성공", response));
    }

    // 회원정보 수정
    @PutMapping("/users")
    public ResponseEntity<ResponseDTO<String>> updateUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request) {

        User user = userDetails.getUser();  // UnauthorizedException 발생 가능
        userService.updateUser(user, request);         // DuplicateNicknameException 발생 가능

        return ResponseEntity.ok(
                ResponseDTO.success(HttpStatus.OK.value(), "회원정보가 성공적으로 변경되었습니다.", null)
        );
    }

    // 회원 탈퇴
    @DeleteMapping("/users")
    public ResponseEntity<ResponseDTO<String>> deleteUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        userService.deleteUser(user);

        return ResponseEntity.ok(
                ResponseDTO.success(HttpStatus.OK.value(), "회원 탈퇴가 완료되었습니다.", null)
        );
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ResponseDTO<String>> logout(
            @RequestBody RefreshTokenRequest request) {
        userService.logout(request.getRefreshToken());
        return ResponseEntity.ok(
                ResponseDTO.success(HttpStatus.OK.value(), "로그아웃 완료", "성공적으로 로그아웃되었습니다."));
    }

    // 상세 정보
    @GetMapping("/users/profile")
    public ResponseEntity<ResponseDTO<UserProfile>> getUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        UserProfile profile = UserProfile.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .role(user.getRole())
                .level(user.getLevel())
                .point(user.getPoint())
                .signature(user.getSignature())
                .socialLoginType(user.getSocialLoginType())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(
                ResponseDTO.success(HttpStatus.OK.value(), "프로필 조회 성공", profile)
        );
    }


    // 로그인한 사용자 확인
    @GetMapping("/users/me")
    public ResponseEntity<ResponseDTO<String>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ResponseDTO.success(HttpStatus.OK.value(), "인증된 사용자", userDetails.getUsername())
        );
    }
}
