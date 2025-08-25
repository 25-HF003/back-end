package com.deeptruth.deeptruth.base.exception;

public class InvalidWatermarkResponseException extends RuntimeException {
    public InvalidWatermarkResponseException(String reason) {
        super("Invalid watermark response: " + reason);
    }
}