package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.base.dto.noise.NoiseFlaskResponseDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
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
    private final WebClient webClient;

    @Value("${flask.noiseServer.url}")
    private String flaskServerUrl; // http://localhost:5002

    @PostMapping
    public ResponseEntity<ResponseDTO<NoiseDTO>> createNoise(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile multipartFile) {
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ResponseDTO.fail(401, "인증이 필요합니다."));
            }

            if (multipartFile.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ResponseDTO.fail(400, "파일이 비어있습니다."));
            }

            // Flask 호출
            ByteArrayResource resource = new ByteArrayResource(multipartFile.getBytes()) {
                @Override
                public String getFilename() {
                    return multipartFile.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("file", resource);

            // Flask API 호출
            NoiseFlaskResponseDTO flaskResult = webClient.post()
                    .uri(flaskServerUrl + "/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(form))
                    .retrieve()
                    .bodyToMono(NoiseFlaskResponseDTO.class)
                    .block();

            if (flaskResult == null) {
                return ResponseEntity.status(500)
                        .body(ResponseDTO.fail(500, "Flask 서버 응답 실패"));
            }

            // S3 업로드
            if (flaskResult.getOriginalFilePath() != null &&
                    flaskResult.getOriginalFilePath().startsWith("data:image/")) {
                String originalUrl = noiseService.uploadBase64ImageToS3(
                        flaskResult.getOriginalFilePath(), user.getUserId(), "original", multipartFile.getOriginalFilename());
                flaskResult.setOriginalFilePath(originalUrl);
            }
            if (flaskResult.getProcessedFilePath() != null &&
                    flaskResult.getProcessedFilePath().startsWith("data:image/")) {
                String processedUrl = noiseService.uploadBase64ImageToS3(
                        flaskResult.getProcessedFilePath(), user.getUserId(), "processed", multipartFile.getOriginalFilename());
                flaskResult.setProcessedFilePath(processedUrl);
            }
            // Service 호출
            NoiseDTO createdNoise = noiseService.createNoise(user.getUserId(), flaskResult, multipartFile.getOriginalFilename());;

            return ResponseEntity.ok(ResponseDTO.success(200, "적대적 노이즈 삽입 성공", createdNoise));

        } catch (Exception e) {
            log.error("❌ 적대적 노이즈 생성 중 오류 발생: ", e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ResponseDTO<Page<NoiseDTO>>> getAllNoises(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NoiseDTO> result = noiseService.getAllResult(user.getUserId(), pageable);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "적대적 노이즈 기록 전체 조회 성공", result)
        );
    }

    @GetMapping("/{noiseId}")
    public ResponseEntity<ResponseDTO<NoiseDTO>> getNoiseById(
            @AuthenticationPrincipal User user,
            @PathVariable Long noiseId) {
        try {
            NoiseDTO noise = noiseService.getSingleResult(user.getUserId(), noiseId);
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
            @AuthenticationPrincipal User user,
            @PathVariable Long noiseId) {
        try {
            noiseService.deleteResult(user.getUserId(), noiseId);
            return ResponseEntity.ok(
                    ResponseDTO.success(200, "적대적 노이즈 삭제 성공", null));
        } catch (Exception e) {
            log.error("적대적 노이즈 삭제 중 오류 발생: ", e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }

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
}