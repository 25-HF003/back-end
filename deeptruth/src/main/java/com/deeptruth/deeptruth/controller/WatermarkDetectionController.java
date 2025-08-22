package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.base.dto.watermarkDetection.DetectResultDTO;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.service.WatermarkDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watermark")
public class WatermarkDetectionController {
    private final WatermarkDetectionService detectionService;

    @PostMapping(value = "/detection", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO> detect(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String taskId) {
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(ResponseDTO.fail(401, "인증이 필요합니다."));
        }
        try {
            DetectResultDTO result = detectionService.detect(user.getUserId(), file, taskId);
            return ResponseEntity.ok(ResponseDTO.success(200, "워터마크 탐지 성공", result));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(404)
                    .body(ResponseDTO.fail(404, ex.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .body(ResponseDTO.fail(502, "서버 오류: " + e.getMessage()));
        }
    }
}
