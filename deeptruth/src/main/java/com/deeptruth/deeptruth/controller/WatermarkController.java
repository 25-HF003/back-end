package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.base.dto.watermark.WatermarkDTO;
import com.deeptruth.deeptruth.service.WatermarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.print.Pageable;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/watermark")
public class WatermarkController {

    private final WatermarkService waterMarkService;


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
