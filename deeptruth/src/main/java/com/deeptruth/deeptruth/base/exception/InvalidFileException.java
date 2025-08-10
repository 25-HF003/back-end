package com.deeptruth.deeptruth.base.exception;

public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String reason) { super("Invalid file: " + reason); }
}