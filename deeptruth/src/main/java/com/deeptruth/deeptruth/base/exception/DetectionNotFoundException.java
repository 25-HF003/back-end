package com.deeptruth.deeptruth.base.exception;

public class DetectionNotFoundException extends RuntimeException {
    public DetectionNotFoundException(Long detectionId, Long userId) {
        super("Deepfake detection not found. id=" + detectionId + ", userId=" + userId);
    }
}