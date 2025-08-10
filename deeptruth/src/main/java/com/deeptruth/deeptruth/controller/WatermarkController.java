package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.base.dto.watermark.WatermarkDTO;
import com.deeptruth.deeptruth.base.dto.watermark.WatermarkFlaskResponseDTO;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.service.UserService;
import com.deeptruth.deeptruth.service.WatermarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/watermark")
public class WatermarkController {

    private final WatermarkService waterMarkService;

    private final UserService userService;
    private final WebClient webClient;

    @Value("${flask.watermarkServer.url}")
    private String flaskServerUrl;

    @PostMapping
    public ResponseEntity<ResponseDTO> insertWatermark(@AuthenticationPrincipal User user, @RequestPart("file") MultipartFile multipartFile, @RequestPart String message){
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ResponseDTO.fail(401, "인증이 필요합니다."));
            }

            ByteArrayResource resource = new ByteArrayResource(multipartFile.getBytes()) {
                @Override
                public String getFilename() {
                    return multipartFile.getOriginalFilename();
                }
            };

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("image", resource);
            builder.part("message", message);

            Mono<WatermarkFlaskResponseDTO> flaskResponseMono = webClient.post()
                    .uri(flaskServerUrl + "/watermark-insert")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(WatermarkFlaskResponseDTO.class);

            WatermarkFlaskResponseDTO flaskResult = flaskResponseMono.block();

            if (flaskResult == null) {
                return ResponseEntity.status(500).body(ResponseDTO.fail(500, "Flask 서버 응답 실패"));
            }

            String base64Image = flaskResult.getImage_base64();
            String waterMarkedImageUrl = null;


            if (base64Image != null && !base64Image.isEmpty()) {
                waterMarkedImageUrl = waterMarkService.uploadBase64ImageToS3(base64Image, user.getUserId());
                flaskResult.setWatermarkedFilePath(waterMarkedImageUrl);
            }

            WatermarkDTO dto = waterMarkService.createWatermark(user.getUserId(), flaskResult);

            return ResponseEntity.ok(ResponseDTO.success(200, "워터마크 삽입 성공", dto));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ResponseDTO> getAllWatermarks(@RequestParam Long userId, @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        Page<WatermarkDTO> result = waterMarkService.getAllResult(userId, pageable);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "워터마크 삽입 기록 전체 조회 성공", result)
        );

    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getWatermark(@PathVariable Long id, @RequestParam Long userId){
        WatermarkDTO result = waterMarkService.getSingleResult(userId, id);
        return ResponseEntity.ok(ResponseDTO.success(200, "워터마크 삽입 기록 조회 성공", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteWatermark(@PathVariable Long id, @RequestParam Long userId){
        waterMarkService.deleteWatermark(userId, id);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "워터마크 삽입 기록 삭제 성공", null)
        );
    }
}
