package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.FlaskResponseDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.service.DeepfakeDetectionService;
import com.deeptruth.deeptruth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/deepfake")
public class DeepfakeDetectionController {

    private final DeepfakeDetectionService deepfakeDetectionService;

    private final UserService userService;

    private final WebClient webClient;

    @Value("${flask.deepfakeServer.url}")
    private String flaskServerUrl;

    @PostMapping
    public ResponseEntity<ResponseDTO> detectVideo(Long userId, @RequestPart("file")MultipartFile multipartFile){
        try {
            if (!userService.existsByUserId(userId)) {
                return ResponseEntity.status(404).body(ResponseDTO.fail(404, "존재하지 않는 사용자입니다."));
            }

            ByteArrayResource resource = new ByteArrayResource(multipartFile.getBytes()) {
                @Override
                public String getFilename() {
                    return multipartFile.getOriginalFilename();
                }
            };

            Mono<FlaskResponseDTO> flaskResponseMono = webClient.post()
                    .uri(flaskServerUrl + "/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("file", resource))
                    .retrieve()
                    .bodyToMono(FlaskResponseDTO.class);

            FlaskResponseDTO flaskResult = flaskResponseMono.block();

            if (flaskResult == null) {
                return ResponseEntity.status(500).body(ResponseDTO.fail(500, "Flask 서버 응답 실패"));
            }

            String base64Image = flaskResult.getBase64Url();
            String imageUrl = null;

            if (base64Image != null && !base64Image.isEmpty()) {
                imageUrl = deepfakeDetectionService.uploadBase64ImageToS3(base64Image, userId);
                flaskResult.setImageUrl(imageUrl);
            }

            DeepfakeDetectionDTO dto = deepfakeDetectionService.createDetection(userId, flaskResult);

            return ResponseEntity.ok(ResponseDTO.success(200, "딥페이크 탐지 결과 수신 성공", dto));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ResponseDTO> getAllDetections(@RequestParam Long userId, @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        Page<DeepfakeDetectionDTO> result = deepfakeDetectionService.getAllResult(userId, pageable);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "딥페이크 탐지 결과 전체 조회 성공", result)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetection(@PathVariable Long id, @RequestParam Long userId) {
        DeepfakeDetectionDTO result = deepfakeDetectionService.getSingleResult(userId, id);
        return ResponseEntity.ok(ResponseDTO.success(200, "딥페이크 탐지 결과 조회 성공", result));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteDetection(@PathVariable Long id, @RequestParam Long userId){
        deepfakeDetectionService.deleteResult(userId, id);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "딥페이크 탐지 결과 삭제 성공", null)
        );
    }
}
