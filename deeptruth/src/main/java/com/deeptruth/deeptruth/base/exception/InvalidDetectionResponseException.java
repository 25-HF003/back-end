package com.deeptruth.deeptruth.base.exception;

public class InvalidDetectionResponseException extends RuntimeException {
    public InvalidDetectionResponseException(String reason) { super("Invalid detection response: " + reason); }
}