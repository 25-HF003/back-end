package com.deeptruth.deeptruth.base.exception;

public class InvalidEnumValueException extends RuntimeException {
    public InvalidEnumValueException(String field, String value) {
        super("Invalid enum value for " + field + ": " + value);
    }
}