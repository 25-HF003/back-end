package com.deeptruth.deeptruth.base.exception;

public class UnsupportedMediaTypeException extends RuntimeException {
    public UnsupportedMediaTypeException(String contentType) {
        super("지원하지 않는 콘텐츠 타입입니다: " + contentType);
    }
}