package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.service.DeepfakeDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/deepfake")
public class DeepfakeDetectionController {

    private final DeepfakeDetectionService deepfakeDetectionService;

    @GetMapping
    public ResponseEntity<ResponseDTO> getAllDetections(){
        List<DeepfakeDetectionDTO> result = deepfakeDetectionService.getAllResult();
        return ResponseEntity.ok(
                ResponseDTO.success(200, "딥페이크 탐지 결과 전체 조회 성공", result)
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteDetection(@PathVariable Long id){
        deepfakeDetectionService.deleteResult(id);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "딥페이크 탐지 결과 삭제 성공", null)
        );
    }
}
