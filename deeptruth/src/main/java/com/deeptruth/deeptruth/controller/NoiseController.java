package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.noise.NoiseCreateRequestDTO;
import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.base.dto.response.ResponseDTO;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.service.NoiseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/noise")
@RequiredArgsConstructor
public class NoiseController {

    private final NoiseService noiseService;

    @GetMapping("/history")
    public ResponseEntity<ResponseDTO<List<NoiseDTO>>> getMyNoiseHistory(
            @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ResponseDTO.fail(401, "인증이 필요합니다"));
            }

            Long userId = user.getUserId();
            String userName = user.getName();

            log.info("사용자 {}({})의 노이즈 이력 조회 요청", userName, userId);

            List<NoiseDTO> history = noiseService.getUserNoiseHistory(userId);

            return ResponseEntity.ok(
                    ResponseDTO.success(200, "노이즈 삽입 이력 조회 성공", history));
        } catch (Exception e) {
            log.error("노이즈 이력 조회 중 오류 발생: ", e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }


    @GetMapping
    public ResponseEntity<List<NoiseDTO>> getAllNoiseRecordsByUserId(@RequestParam Long userId) {
        List<NoiseDTO> noiseRecords = noiseService.getUserNoiseHistory(userId);
        return ResponseEntity.ok(noiseRecords);
    }

    @PostMapping
    public ResponseEntity<ResponseDTO<NoiseDTO>> createNoise(
            @AuthenticationPrincipal User user,
            @RequestBody NoiseCreateRequestDTO request) {
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ResponseDTO.fail(401, "인증이 필요합니다"));
            }

            Long userId = user.getUserId();
            String userName = user.getName();

            log.info("사용자 {}({})의 노이즈 생성 요청: epsilon={}",
                    userName, userId, request.getEpsilon());

            NoiseDTO createdNoise = noiseService.createNoise(userId, request);

            // 성공 응답 반환
            return ResponseEntity.status(201)
                    .body(ResponseDTO.success(201, "노이즈 생성 성공", createdNoise));

        } catch (IllegalArgumentException e) {
            // 클라이언트 오류 (400)
            return ResponseEntity.status(400)
                    .body(ResponseDTO.fail(400, e.getMessage()));
        } catch (Exception e) {
            // 서버 오류 로깅 및 응답
            log.error("노이즈 생성 중 오류 발생: ", e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }

    @GetMapping("/{noiseId}")
    public ResponseEntity<ResponseDTO<NoiseDTO>> getNoiseById(
            @AuthenticationPrincipal User user,
            @PathVariable Long noiseId) {
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ResponseDTO.fail(401, "인증이 필요합니다"));
            }

            NoiseDTO noise = noiseService.getNoiseById(user.getUserId(), noiseId);

            return ResponseEntity.ok(
                    ResponseDTO.success(200, "노이즈 조회 성공", noise));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ResponseDTO.fail(404, e.getMessage()));
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
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ResponseDTO.fail(401, "인증이 필요합니다."));
            }

            noiseService.deleteNoise(user.getUserId(), noiseId);

            return ResponseEntity.ok(
                    ResponseDTO.success(200, "노이즈 삭제 성공", null));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ResponseDTO.fail(404, e.getMessage()));
        } catch (Exception e) {
            log.error("노이즈 삭제 중 오류 발생: ", e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.fail(500, "서버 오류: " + e.getMessage()));
        }
    }


}