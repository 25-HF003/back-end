package com.deeptruth.deeptruth.base.exception;

import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 404 - 리소스 없음
    @ExceptionHandler({UserNotFoundException.class, DetectionNotFoundException.class})
    public ResponseEntity<ResponseDTO> handleNotFound(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseDTO.fail(404, ex.getMessage()));
    }

    // 400 - 잘못된 요청
    @ExceptionHandler({InvalidFileException.class, ImageDecodingException.class})
    public ResponseEntity<ResponseDTO> handleBadRequest(RuntimeException ex) {
        return ResponseEntity
                .badRequest()
                .body(ResponseDTO.fail(400, ex.getMessage()));
    }

    // 422 - 처리할 수 없는 엔티티 (유효성은 맞지만 비즈니스 규칙 위반)
    @ExceptionHandler(InvalidDetectionResponseException.class)
    public ResponseEntity<ResponseDTO> handleUnprocessableEntity(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ResponseDTO.fail(422, ex.getMessage()));
    }

    // 5xx - 서버/외부 연동 문제
    @ExceptionHandler({StorageException.class, ExternalServiceException.class})
    public ResponseEntity<ResponseDTO> handleServerError(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ResponseDTO.fail(502, ex.getMessage()));
    }

    // 그 외 모든 예외 - 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> handleGeneral(Exception ex) {
        // 로그 남기기
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDTO.fail(500, "서버 내부 오류가 발생했습니다."));
    }
}
