package com.deeptruth.deeptruth.constants;

public class SignupConstants {
    // 정규식 패턴
    public static final String LOGIN_ID_PATTERN = "^[a-z0-9]{6,20}$";
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*\\W).{8,30}$";
    public static final String NICKNAME_PATTERN = "^[가-힣a-zA-Z0-9]{2,15}$";

    // 에러 메시지
    public static final String LOGIN_ID_ERROR_MESSAGE = "아이디는 6~20자의 영소문자 및 숫자만 가능합니다.";
    public static final String PASSWORD_ERROR_MESSAGE = "비밀번호는 8~30자, 영대/소문자·숫자·특수문자를 모두 포함해야 합니다.";
    public static final String NICKNAME_ERROR_MESSAGE = "닉네임은 2~15자, 공백 및 특수문자를 제외한 한글/영문/숫자만 가능합니다.";

    // 중복 검사 에러 메시지
    public static final String DUPLICATE_LOGIN_ID_MESSAGE = "이미 사용 중인 아이디입니다.";
    public static final String DUPLICATE_NICKNAME_MESSAGE = "이미 사용 중인 닉네임입니다.";
    public static final String DUPLICATE_EMAIL_MESSAGE = "이미 사용 중인 이메일입니다.";

    // 성공 메시지
    public static final String SIGNUP_SUCCESS_MESSAGE = "회원가입이 완료되었습니다.";
}
