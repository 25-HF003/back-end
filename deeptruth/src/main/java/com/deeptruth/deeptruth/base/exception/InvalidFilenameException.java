package com.deeptruth.deeptruth.base.exception;

public class InvalidFilenameException extends RuntimeException {
    public InvalidFilenameException(String filename) {
        super("파일 이름이 유효하지 않습니다: " + filename);
    }
}
