package com.deeptruth.deeptruth.base.exception;

public class S3UploadFailedException extends RuntimeException {
    public S3UploadFailedException(String key, Throwable cause) {
        super("S3 업로드에 실패했습니다. key=" + key, cause);
    }
}