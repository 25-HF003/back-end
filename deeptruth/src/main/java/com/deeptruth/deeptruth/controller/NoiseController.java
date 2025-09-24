package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.base.dto.noise.NoiseFlaskResponseDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.config.CustomUserDetails;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.service.NoiseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/noise")
@RequiredArgsConstructor
public class NoiseController {

    private final NoiseService noiseService;

    @PostMapping
    public ResponseEntity<ResponseDTO<NoiseDTO>> createNoise(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile multipartFile,
            @RequestParam(value = "mode", defaultValue = "auto") String mode,
            @RequestParam(value = "level", defaultValue = "2") Integer level,
            @RequestParam(value = "taskId", required = false)  String taskId) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401)
                        .body(ResponseDTO.fail(401, "인증이 필요합니다."));
            }

            if (multipartFile.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ResponseDTO.fail(400, "파일이 필요합니다."));
            }

            NoiseDTO result = noiseService.createNoise(
                    userDetails.getUserId(),
                    userDetails.getUser().getLoginId(),
                    multipartFile,
                    mode,
                    level,
                    taskId
            );

            return ResponseEntity.ok(ResponseDTO.success(200, "적대적 노이즈 삽입 성공", result));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터", e);
            return ResponseEntity.status(400)
                    .body(ResponseDTO.fail(400, e.getMessage()));
        } catch (Exception e) {
            log.error("적대적 노이즈 삽입 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.fail(500, "서버 내부 오류가 발생했습니다."));
        }
    }

    @GetMapping
    public ResponseEntity<ResponseDTO<Page<NoiseDTO>>> getAllNoises(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NoiseDTO> result = noiseService.getAllResult(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "적대적 노이즈 기록 전체 조회 성공", result)
        );
    }

    @GetMapping("/{noiseId}")
    public ResponseEntity<ResponseDTO<NoiseDTO>> getNoiseById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long noiseId) {
        try {
            NoiseDTO noise = noiseService.getSingleResult(userDetails.getUserId(), noiseId);
            return ResponseEntity.ok(
                    ResponseDTO.success(200, "적대적 노이즈 조회 성공", noise));
        } catch (Exception e) {
            log.error("노이즈 조회 중 오류 발생: ", e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{noiseId}")
    public ResponseEntity<ResponseDTO<Void>> deleteNoise(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long noiseId) {
        try {
            noiseService.deleteResult(userDetails.getUserId(), noiseId);
            return ResponseEntity.ok(
                    ResponseDTO.success(200, "적대적 노이즈 삭제 성공", null));
        } catch (Exception e) {
            log.error("적대적 노이즈 삭제 중 오류 발생: ", e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }

    /*
    @GetMapping("/history")
    public ResponseEntity<ResponseDTO<List<NoiseDTO>>> getMyNoiseHistory(
            @AuthenticationPrincipal User user) {
        try {
            List<NoiseDTO> history = noiseService.getUserNoiseHistory(user.getUserId());
            return ResponseEntity.ok(
                    ResponseDTO.success(200, "적대적 노이즈 삽입 이력 조회 성공", history));
        } catch (Exception e) {
            log.error("적대적 노이즈 이력 조회 중 오류 발생: ", e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }
     */
}