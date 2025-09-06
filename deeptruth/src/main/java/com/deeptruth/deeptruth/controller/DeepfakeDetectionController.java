package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionListDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.FlaskResponseDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.config.CustomUserDetails;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deepfake")
public class DeepfakeDetectionController {

    private final DeepfakeDetectionService deepfakeDetectionService;

    @PostMapping
    public ResponseEntity<ResponseDTO> detectVideo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file")MultipartFile multipartFile,
            @RequestParam(required = false) Map<String, String> params){
        try {
            Map<String, String> form = (params == null) ? new HashMap<>() : params;
            DeepfakeDetectionDTO dto = deepfakeDetectionService.createDetection(userDetails.getUserId(), multipartFile, form);
            return ResponseEntity.ok(ResponseDTO.success(200, "딥페이크 탐지 결과 수신 성공", dto));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ResponseDTO> getAllDetections(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        Page<DeepfakeDetectionListDTO> result = deepfakeDetectionService.getAllResult(userDetails.getUserId(), pageable);

        return ResponseEntity.ok(
                ResponseDTO.success(200, "딥페이크 탐지 결과 전체 조회 성공", result)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetection(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        DeepfakeDetectionDTO result = deepfakeDetectionService.getSingleResult(userDetails.getUserId(), id);
        return ResponseEntity.ok(ResponseDTO.success(200, "딥페이크 탐지 결과 조회 성공", result));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteDetection(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails){
        deepfakeDetectionService.deleteResult(userDetails.getUserId(), id);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "딥페이크 탐지 결과 삭제 성공", null)
        );
    }
}
