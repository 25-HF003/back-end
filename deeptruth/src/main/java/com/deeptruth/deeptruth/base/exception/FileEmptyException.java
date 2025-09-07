package com.deeptruth.deeptruth.base.exception;

public class FileEmptyException  extends RuntimeException {
    public FileEmptyException() { super("업로드할 파일이 비어 있습니다."); }
}