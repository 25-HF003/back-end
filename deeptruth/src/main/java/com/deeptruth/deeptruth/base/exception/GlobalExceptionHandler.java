package com.deeptruth.deeptruth.base.exception;

import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 400 - 잘못된 요청
    @ExceptionHandler({InvalidFileException.class, ImageDecodingException.class,
            IllegalArgumentException.class, FileEmptyException.class, InvalidFilenameException.class, InvalidEnumValueException.class})
    public ResponseEntity<ResponseDTO> handleBadRequest(RuntimeException ex) {
        log.info("[400] {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ResponseDTO.fail(400, ex.getMessage()));
    }

    // 401 - 인증 실패
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ResponseDTO> handleUserNotFoundException(RuntimeException ex) {
        log.info("[401] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseDTO.fail(401, "존재하지 않는 회원입니다."));
    }

    @ExceptionHandler({InvalidPasswordException.class})
    public ResponseEntity<ResponseDTO> handleInvalidPassword(RuntimeException ex) {
        log.info("[401] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseDTO.fail(401, ex.getMessage()));
    }

    // 403 - 권한 부족
    @ExceptionHandler({UnauthorizedOperationException.class})
    public ResponseEntity<ResponseDTO> handleAuthorizationFailure(RuntimeException ex) {
        log.info("[403] Authorization failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseDTO.fail(403, ex.getMessage()));
    }

    // 404 - 리소스 없음
    @ExceptionHandler({DetectionNotFoundException.class, WatermarkNotFoundException.class, NoiseNotFoundException.class})
    public ResponseEntity<ResponseDTO> handleResourceNotFound(RuntimeException ex) {
        log.info("[404] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseDTO.fail(404, ex.getMessage()));
    }

    // 409 - 중복 리소스
    @ExceptionHandler({DuplicateEmailException.class, DuplicateNicknameException.class,
            DuplicateLoginIdException.class})
    public ResponseEntity<ResponseDTO> handleDuplicateResource(RuntimeException ex) {
        log.info("[409] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ResponseDTO.fail(409, ex.getMessage()));
    }

    // 415 Unsupported Media Type
    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ResponseEntity<ResponseDTO<Void>> handleUnsupported(UnsupportedMediaTypeException ex) {
        log.info("[415] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ResponseDTO.fail(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), ex.getMessage()));
    }

    // 422 - 처리할 수 없는 엔티티 (유효성은 맞지만 비즈니스 규칙 위반)
    @ExceptionHandler({InvalidDetectionResponseException.class, InvalidWatermarkResponseException.class, DataCorruptionException.class, DataMappingException.class})
    public ResponseEntity<ResponseDTO> handleUnprocessableEntity(RuntimeException ex) {
        log.info("[422] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ResponseDTO.fail(422, ex.getMessage()));
    }

    // 5xx - 서버/외부 연동 문제
    @ExceptionHandler({StorageException.class, ExternalServiceException.class, S3UploadFailedException.class})
    public ResponseEntity<ResponseDTO> handleServerError(RuntimeException ex) {
        log.error("[502] {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ResponseDTO.fail(502, ex.getMessage()));
    }

    // 500 - 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> handleGeneral(Exception ex) {
        log.error("[500] {}", ex.getMessage(), ex);
        // 로그 남기기
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDTO.fail(500, "서버 내부 오류가 발생했습니다."));
    }

}
