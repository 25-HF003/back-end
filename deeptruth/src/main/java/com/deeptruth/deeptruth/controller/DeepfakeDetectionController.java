package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionListDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.base.dto.websocket.TaskAcceptedDTO;
import com.deeptruth.deeptruth.config.CustomUserDetails;
import com.deeptruth.deeptruth.service.DeepfakeDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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
            Map<String, String> form = (params == null) ? new HashMap<>() : params;
            TaskAcceptedDTO accepted =
                deepfakeDetectionService.createDetectionAsync(userDetails.getUserId(), multipartFile, form);

        return ResponseEntity.accepted().body(ResponseDTO.success(202, "작업 접수됨", accepted));
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
