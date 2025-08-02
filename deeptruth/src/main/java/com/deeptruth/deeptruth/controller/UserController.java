package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.login.LoginRequestDTO;
import com.deeptruth.deeptruth.base.dto.login.LoginResponse;
import com.deeptruth.deeptruth.base.dto.login.RefreshTokenRequest;
import com.deeptruth.deeptruth.base.dto.signup.SignupRequestDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.deeptruth.deeptruth.constants.LoginConstants.*;
import static com.deeptruth.deeptruth.constants.SignupConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
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

    @PostMapping("/login")
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

    @PostMapping("/refresh")
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

    @PostMapping("/logout")
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
}
