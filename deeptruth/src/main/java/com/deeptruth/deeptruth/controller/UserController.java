package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.login.LoginRequestDTO;
import com.deeptruth.deeptruth.base.dto.login.LoginResponse;
import com.deeptruth.deeptruth.base.dto.login.RefreshTokenRequest;
import com.deeptruth.deeptruth.base.dto.signup.SignupRequestDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        try {
            LoginResponse loginResponse = userService.login(request);

            return ResponseEntity.ok(
                    ResponseDTO.success(
                            HttpStatus.OK.value(),
                            LOGIN_SUCCESS_MESSAGE,
                            loginResponse
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ResponseDTO.fail(
                            HttpStatus.BAD_REQUEST.value(),
                            e.getMessage()
                    )
            );
        }
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ResponseDTO<LoginResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            LoginResponse response = userService.refreshAccessToken(request.getRefreshToken());
            return ResponseEntity.ok(
                    ResponseDTO.success(HttpStatus.OK.value(), "토큰 재발급 성공", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ResponseDTO.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ResponseDTO<String>> logout(@RequestBody RefreshTokenRequest request) {
        try {
            userService.logout(request.getRefreshToken());
            return ResponseEntity.ok(
                    ResponseDTO.success(HttpStatus.OK.value(), "로그아웃 완료", "성공적으로 로그아웃되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ResponseDTO.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    // 상세 정보
    @GetMapping("/users/profile")
    public ResponseEntity<ResponseDTO<UserProfile>> getUserProfile(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();

            UserProfile profile = UserProfile.builder()
                    .userId(user.getUserId())
                    .loginId(user.getLoginId())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();

            return ResponseEntity.ok(
                    ResponseDTO.success(HttpStatus.OK.value(), "프로필 조회 성공", profile)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.fail(HttpStatus.BAD_REQUEST.value(), "프로필 조회 실패"));
        }
    }

    // 로그인한 사용자 확인
    @GetMapping("/users/me")
    public ResponseEntity<ResponseDTO<String>> getCurrentUser(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                ResponseDTO.success(HttpStatus.OK.value(), "인증된 사용자", user.getLoginId())
        );
    }
}
