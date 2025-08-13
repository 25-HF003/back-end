package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.base.dto.watermark.InsertResultDTO;
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
@RequestMapping("/api/watermark")
public class WatermarkController {

    private final WatermarkService waterMarkService;

    private final UserService userService;
    private final WebClient webClient;

    @PostMapping
    public ResponseEntity<ResponseDTO> insertWatermark(@AuthenticationPrincipal User user, @RequestPart("file") MultipartFile multipartFile, @RequestPart String message){
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ResponseDTO.fail(401, "인증이 필요합니다."));
            }

            InsertResultDTO result = waterMarkService.insert(user.getUserId(), multipartFile, message);
            return ResponseEntity.ok(ResponseDTO.success(200, "워터마크 삽입 성공", result));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(400, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ResponseDTO> getAllWatermarks(@AuthenticationPrincipal User user, @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){


        Page<InsertResultDTO> result = waterMarkService.getAllResult(user.getUserId(), pageable);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "워터마크 삽입 기록 전체 조회 성공", result)
        );

    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getWatermark(@PathVariable Long id, @AuthenticationPrincipal User user){

        InsertResultDTO result = waterMarkService.getSingleResult(user.getUserId(), id);
        return ResponseEntity.ok(ResponseDTO.success(200, "워터마크 삽입 기록 조회 성공", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteWatermark(@PathVariable Long id, @AuthenticationPrincipal User user){

        waterMarkService.deleteWatermark(user.getUserId(), id);
        return ResponseEntity.ok(
                ResponseDTO.success(200, "워터마크 삽입 기록 삭제 성공", null)
        );
    }
}
