package com.deeptruth.deeptruth.base.exception;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String reason) { super("External service error: " + reason); }
}