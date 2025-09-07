package com.deeptruth.deeptruth.base.exception;

// 필수 필드 누락/엔티티 변환 불가/데이터 불일치
public class DataMappingException extends RuntimeException {
    public DataMappingException(String message) { super(message); }
}