package com.deeptruth.deeptruth.base.exception;

public class InvalidNoiseResponseException extends RuntimeException {
    public InvalidNoiseResponseException(String reason) {
        super("Invalid Adversarial-Noise response: " + reason);
    }
}