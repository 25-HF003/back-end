package com.deeptruth.deeptruth.base.dto.user;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    // 일반+소셜
    private String nickname;

    // 일반
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    private String currentPassword;
    private String newPassword;
    private String newPasswordConfirm;
}
