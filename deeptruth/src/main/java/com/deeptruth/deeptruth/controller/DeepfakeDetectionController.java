package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.service.DeepfakeDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/deepfake")
public class DeepfakeDetectionController {

    private final DeepfakeDetectionService deepfakeDetectionService;

    @PostMapping
    public ResponseEntity<ResponseDTO> uploadVideo(Long userId, @RequestPart("file")MultipartFile multipartFile){
        DeepfakeDetectionDTO result = deepfakeDetectionService.uploadVideo(userId, multipartFile);

        return ResponseEntity.ok(
                ResponseDTO.success(200, "딥페이크 탐지 영상 업로드 성공", result)
        );
    }

    @GetMapping
    public ResponseEntity<ResponseDTO> getAllDetections(@RequestParam Long userId){
        List<DeepfakeDetectionDTO> result = deepfakeDetectionService.getAllResult(userId);
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
