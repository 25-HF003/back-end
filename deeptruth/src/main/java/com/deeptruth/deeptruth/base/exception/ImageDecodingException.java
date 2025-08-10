package com.deeptruth.deeptruth.base.exception;

public class ImageDecodingException extends RuntimeException {
    public ImageDecodingException(String reason) { super("Failed to decode Base64 image: " + reason); }
}