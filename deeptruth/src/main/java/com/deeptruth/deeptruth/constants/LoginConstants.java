package com.deeptruth.deeptruth.constants;

public class LoginConstants {
    // 에러 메시지
    public static final String EMPTY_LOGIN_ID_MESSAGE = "아이디를 입력해주세요.";
    public static final String EMPTY_PASSWORD_MESSAGE = "비밀번호를 입력해주세요.";
    public static final String USER_NOT_FOUND_MESSAGE = "존재하지 않는 아이디입니다.";
    public static final String PASSWORD_MISMATCH_MESSAGE = "비밀번호가 일치하지 않습니다.";

    // 성공 메시지
    public static final String LOGIN_SUCCESS_MESSAGE = "로그인이 완료되었습니다.";

    // JWT 클레임
    public static final String USER_ID_CLAIM = "userId";
    public static final String ROLE_CLAIM = "role";
}