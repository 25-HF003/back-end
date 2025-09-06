package com.deeptruth.deeptruth.base.exception;

public class DuplicateLoginIdException extends RuntimeException {
    public DuplicateLoginIdException(String message) {
        super(message);
    }
}
