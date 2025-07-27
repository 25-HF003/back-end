package com.deeptruth.deeptruth.base.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDTO {
    private String name;           // 이름
    private String loginId;        // 아이디
    private String password;       // 비밀번호
    private String passwordConfirm; // 비밀번호 확인
    private String nickname;       // 닉네임
    private String email;          // 이메일
}
