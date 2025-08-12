package com.deeptruth.deeptruth.base.exception;

public class UserNotFoundException  extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User not found. id=" + userId);
    }
}
