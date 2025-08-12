package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.FlaskResponseDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.entity.User;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deepfake")
public class DeepfakeDetectionController {

    private final DeepfakeDetectionService deepfakeDetectionService;

    private final UserService userService;

    private final WebClient webClient;

    @Value("${flask.deepfakeServer.url}")
    private String flaskServerUrl;

    @PostMapping
    public ResponseEntity<ResponseDTO> detectVideo(
            @AuthenticationPrincipal User user,
            @RequestPart("file")MultipartFile multipartFile,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false, defaultValue = "default") String mode,           // default | precision
            @RequestParam(required = false) Boolean useTta,
            @RequestParam(required = false) Boolean useIllum,
            @RequestParam(required = false) String detector,                                 // auto | dlib | dnn
            @RequestParam(required = false) Integer smoothWindow,
            @RequestParam(required = false) Integer minFace,
            @RequestParam(required = false) Integer sampleCount){
        try {

            if (taskId == null || taskId.isBlank()) {
                taskId = java.util.UUID.randomUUID().toString();
            }

            ByteArrayResource resource = new ByteArrayResource(multipartFile.getBytes()) {
                @Override
                public String getFilename() {
                    return multipartFile.getOriginalFilename();
                }
            };

            org.springframework.http.client.MultipartBodyBuilder mb = new org.springframework.http.client.MultipartBodyBuilder();
            mb.part("file", resource)
                    .filename(resource.getFilename())
                    .contentType(multipartFile.getContentType() != null ? MediaType.parseMediaType(multipartFile.getContentType())
                            : MediaType.APPLICATION_OCTET_STREAM);
            mb.part("taskId", taskId);
            // 옵션은 null 아닐 때만 전송 (Flask에서 preset 적용)
            if (mode != null)             mb.part("mode", mode);
            if (useTta != null)           mb.part("use_tta", String.valueOf(useTta));
            if (useIllum != null)         mb.part("use_illum", String.valueOf(useIllum));
            if (detector != null)         mb.part("detector", detector);
            if (smoothWindow != null)     mb.part("smooth_window", String.valueOf(smoothWindow));
            if (minFace != null)          mb.part("min_face", String.valueOf(minFace));
            if (sampleCount != null)      mb.part("sample_count", String.valueOf(sampleCount));


            FlaskResponseDTO flaskResult = webClient.post()
                    .uri(flaskServerUrl + "/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(mb.build()))
                    .retrieve()
                    .bodyToMono(FlaskResponseDTO.class)
                    .block();

            if (flaskResult == null) {
                return ResponseEntity.status(500).body(ResponseDTO.fail(500, "Flask 서버 응답 실패"));
            }

            String base64Image = flaskResult.getBase64Url();
            String imageUrl = null;

            if (base64Image != null && !base64Image.isEmpty()) {
                imageUrl = deepfakeDetectionService.uploadBase64ImageToS3(base64Image, user.getUserId());
                flaskResult.setImageUrl(imageUrl);
            }

            DeepfakeDetectionDTO dto = deepfakeDetectionService.createDetection(user.getUserId(), flaskResult);

            return ResponseEntity.ok(ResponseDTO.success(200, "딥페이크 탐지 결과 수신 성공", dto));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ResponseDTO> getAllDetections(@AuthenticationPrincipal User user, @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        Page<DeepfakeDetectionDTO> result = deepfakeDetectionService.getAllResult(user.getUserId(), pageable);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "딥페이크 탐지 결과 전체 조회 성공", result)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetection(@PathVariable Long id, @AuthenticationPrincipal User user) {
        DeepfakeDetectionDTO result = deepfakeDetectionService.getSingleResult(user.getUserId(), id);
        return ResponseEntity.ok(ResponseDTO.success(200, "딥페이크 탐지 결과 조회 성공", result));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteDetection(@PathVariable Long id, @AuthenticationPrincipal User user){
        deepfakeDetectionService.deleteResult(user.getUserId(), id);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "딥페이크 탐지 결과 삭제 성공", null)
        );
    }
}
