package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.SignupRequestDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ResponseDTO<Void>> signup(@RequestBody SignupRequestDTO request) {
        userService.signup(request);

        return ResponseEntity.ok(
                ResponseDTO.success(
                        HttpStatus.OK.value(),
                        "회원가입이 완료되었습니다.",
                        null
                )
        );
    }
}
