package com.deeptruth.deeptruth.base.dto.login;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String accessToken;   // Access Token (1시간)
    private String refreshToken;  // Refresh Token (2주)
}
