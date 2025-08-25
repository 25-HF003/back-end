package com.deeptruth.deeptruth.base.exception;

public class WatermarkNotFoundException extends RuntimeException {
    public WatermarkNotFoundException(Long watermarkId, Long userId) {
        super("Watermark not found. id=" + watermarkId + ", userId=" + userId);
    }
}