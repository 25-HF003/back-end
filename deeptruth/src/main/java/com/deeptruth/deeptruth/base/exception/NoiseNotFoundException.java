package com.deeptruth.deeptruth.base.exception;

public class NoiseNotFoundException extends RuntimeException {
    public NoiseNotFoundException(Long NoiseId, Long userId) {
        super("Adversarial-Noise not found. id=" + NoiseId + ", userId=" + userId);
    }
}