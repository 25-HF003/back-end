package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.SignupRequestDTO;
import com.deeptruth.deeptruth.base.dto.SignupResponseDTO;
import com.deeptruth.deeptruth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDTO> signup(@RequestBody SignupRequestDTO request) {
        userService.signup(request);

        SignupResponseDTO response = new SignupResponseDTO("회원가입이 완료되었습니다.");
        return ResponseEntity.ok(response);
    }
}
