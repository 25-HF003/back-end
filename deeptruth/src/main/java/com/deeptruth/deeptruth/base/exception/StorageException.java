package com.deeptruth.deeptruth.base.exception;

public class StorageException extends RuntimeException {
    public StorageException(String reason, Throwable cause) { super("Storage error: " + reason, cause); }
}
