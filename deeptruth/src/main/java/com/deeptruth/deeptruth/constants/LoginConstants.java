package com.deeptruth.deeptruth.constants;

public class LoginConstants {
    // 에러 메시지
    public static final String EMPTY_LOGIN_ID_MESSAGE = "아이디를 입력해주세요.";
    public static final String EMPTY_PASSWORD_MESSAGE = "비밀번호를 입력해주세요.";
    public static final String USER_NOT_FOUND_MESSAGE = "존재하지 않는 회원입니다.";
    public static final String PASSWORD_MISMATCH_MESSAGE = "비밀번호가 일치하지 않습니다.";

    // 성공 메시지
    public static final String LOGIN_SUCCESS_MESSAGE = "로그인이 완료되었습니다.";

    // 토큰 관련 에러 메시지
    public static final String INVALID_REFRESH_TOKEN_MESSAGE = "유효하지 않은 Refresh Token입니다.";
    public static final String EXPIRED_REFRESH_TOKEN_MESSAGE = "만료된 Refresh Token입니다.";
    public static final String MISSING_REFRESH_TOKEN_MESSAGE = "Refresh Token이 필요합니다.";
    public static final String NOT_FOUND_REFRESH_TOKEN_MESSAGE = "존재하지 않는 Refresh Token입니다.";
    public static final String LOGOUT_INVALID_TOKEN_MESSAGE = "이미 로그아웃되었거나 존재하지 않는 Refresh Token입니다.";

    // JWT 클레임
    public static final String USER_ID_CLAIM = "userId";
    public static final String ROLE_CLAIM = "role";
}