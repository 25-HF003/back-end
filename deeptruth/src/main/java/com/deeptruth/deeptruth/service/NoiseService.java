package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.base.dto.noise.NoiseFlaskResponseDTO;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.Noise;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NoiseService {

    private final NoiseRepository noiseRepository;
    private final UserRepository userRepository;
    private final AmazonS3Service amazonS3Service;
    private final ActiveTaskService activeTaskService;
    private final WebClient webClient;

    @Value("${flask.noiseServer.url}")
    private String flaskServerUrl;

    public NoiseDTO createNoise(Long userId, String loginId, MultipartFile multipartFile,
                                String mode, Integer level, String taskId) {

        // 1. taskId 생성
        if (taskId == null || taskId.isBlank()) {
            taskId = UUID.randomUUID().toString();
        }

        // 2. 비즈니스 검증
        validateBusinessParameters(multipartFile, mode, level);

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 4. 웹소켓 세션 관리
        activeTaskService.registerTask(loginId, taskId);

        try {
            // 5. Flask API 호출
            NoiseFlaskResponseDTO flaskResponse = callFlaskAPI(multipartFile, mode, level, taskId, loginId);

            // 6. 이미지 후처리 (S3 업로드)
            processImageUploads(flaskResponse, userId, multipartFile.getOriginalFilename());

            // 7. 데이터베이스 저장
            return saveNoiseEntity(user, flaskResponse, multipartFile.getOriginalFilename());

        } finally {
            // 8. 세션 정리
            activeTaskService.deregisterTask(loginId);
        }
    }

    // 비즈니스 파라미터 검증
    private void validateBusinessParameters(MultipartFile file, String mode, Integer level) {

        // 파일 검증
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 필요합니다.");
        }

        // 파일 크기 검증 (10MB 제한)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }

        // 모드 검증
        if (!mode.equals("auto") && !mode.equals("precision")) {
            throw new IllegalArgumentException("mode는 'auto' 또는 'precision'이어야 합니다.");
        }

        // 레벨 검증
        if ("precision".equals(mode) && (level < 1 || level > 4)) {
            throw new IllegalArgumentException("precision 모드에서 level은 1-4 사이여야 합니다.");
        }
    }

    // Flask 호출
    private NoiseFlaskResponseDTO callFlaskAPI(MultipartFile multipartFile, String mode,
                                               Integer level, String taskId, String loginId) {
        try {
            log.info("Flask API 호출 시작 - taskId: {}, loginId: {}", taskId, loginId);

            // 파일 리소스 준비
            ByteArrayResource resource = new ByteArrayResource(multipartFile.getBytes()) {
                @Override
                public String getFilename() {
                    return multipartFile.getOriginalFilename();
                }
            };

            // 요청 데이터 구성
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", resource);
            builder.part("mode", mode);
            builder.part("level", level);
            builder.part("taskId", taskId);
            builder.part("loginId", loginId);

            // Flask API 호출
            NoiseFlaskResponseDTO response = webClient.post()
                    .uri(flaskServerUrl + "/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(NoiseFlaskResponseDTO.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Flask 서버에서 응답을 받지 못했습니다.");
            }

            log.info("Flask API 호출 성공 - taskId: {}, attackSuccess: {}",
                    response.getTaskId(), response.getAttackSuccess());

            return response;

        } catch (Exception e) {
            log.error("Flask API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("Flask API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 이미지 S3 업로드 처리
    private void processImageUploads(NoiseFlaskResponseDTO flaskResult, Long userId, String filename) {

        // 원본 이미지 업로드
        if (flaskResult.getOriginalFilePath() != null &&
                flaskResult.getOriginalFilePath().startsWith("data:image")) {

            String originalUrl = uploadBase64ImageToS3(
                    flaskResult.getOriginalFilePath(), userId, "original", filename
            );
            flaskResult.setOriginalFilePath(originalUrl);
        }

        // 처리된 이미지 업로드
        if (flaskResult.getProcessedFilePath() != null &&
                flaskResult.getProcessedFilePath().startsWith("data:image")) {

            String processedUrl = uploadBase64ImageToS3(
                    flaskResult.getProcessedFilePath(), userId, "processed", filename
            );
            flaskResult.setProcessedFilePath(processedUrl);
        }
    }

    // 노이즈 엔티티 저장
    private NoiseDTO saveNoiseEntity(User user, NoiseFlaskResponseDTO flaskResponse, String originalFileName) {

        // Flask 응답 검증
        if (flaskResponse.getAttackSuccess() == null) {
            throw new RuntimeException("Flask 응답이 유효하지 않습니다.");
        }

        // 파일명 생성
        String userFileName = generateFileName(originalFileName);

        // 엔티티 생성
        Noise noise = Noise.builder()
                .user(user)
                .originalFileName(originalFileName)
                .fileName(userFileName)
                .originalFilePath(flaskResponse.getOriginalFilePath())
                .processedFilePath(flaskResponse.getProcessedFilePath())
                .epsilon(flaskResponse.getEpsilon())
                .attackSuccess(flaskResponse.getAttackSuccess())
                .originalPrediction(flaskResponse.getOriginalPrediction())
                .adversarialPrediction(flaskResponse.getAdversarialPrediction())
                .originalConfidence(flaskResponse.getOriginalConfidence())
                .adversarialConfidence(flaskResponse.getAdversarialConfidence())
                .mode(flaskResponse.getMode())
                .level(flaskResponse.getLevel())
                .build();

        // 데이터베이스 저장
        noiseRepository.save(noise);

        log.info("적대적 노이즈 엔티티 저장 완료 - 사용자: {}, 파일: {}",
                user.getLoginId(), originalFileName);

        // DTO 변환 후 반환
        return NoiseDTO.fromEntityWithFlaskData(noise, flaskResponse);
    }

    // S3 업로드 메소드
    public String uploadBase64ImageToS3(String base64Image, Long userId, String type, String originalFileName) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new ImageDecodingException("empty string");
        }

        // data:image/png;base64, 제거
        String cleanBase64 = base64Image;
        if (base64Image.startsWith("data:image/")) {
            cleanBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
        }

        final byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(cleanBase64);
            log.info("Base64 디코딩 성공, 크기: {} bytes", decodedBytes.length);
        } catch (IllegalArgumentException e) {
            String preview = base64Image.length() > 50 ? base64Image.substring(0, 50) + "..." : base64Image;
            log.error("유효하지 않은 Base64 문자 발견: [{}]", preview);
            throw new ImageDecodingException("Failed to decode Base64 image: Invalid Base64 characters detected");
        }

        try (InputStream inputStream = new ByteArrayInputStream(decodedBytes)) {
            // S3에는 UUID로 저장 (충돌 방지)
            String key = "noise/" + userId + "/" + type + "/" + UUID.randomUUID() + ".jpg";
            String result = amazonS3Service.uploadBase64Image(inputStream, key);
            log.info("S3 업로드 성공: {}", result);
            return result;
        } catch (IOException e) {
            log.error("InputStream 처리 실패: {}", e.getMessage());
            throw new StorageException("failed to process image stream", e);
        } catch (Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new StorageException("failed to upload image to S3", e);
        }
    }

    public Page<NoiseDTO> getAllResult(Long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return noiseRepository.findByUser_UserId(userId, pageable)
                .map(NoiseDTO::fromEntity);
    }

    public NoiseDTO getSingleResult(Long userId, Long noiseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Noise noise = noiseRepository.findByNoiseIdAndUser(noiseId, user)
                .orElseThrow(() -> new NoiseNotFoundException(noiseId, userId));

        return NoiseDTO.fromEntity(noise);
    }

    public void deleteResult(Long userId, Long noiseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        int deleted = noiseRepository.deleteByNoiseIdAndUser(noiseId, user);
        if (deleted == 0) {
            throw new NoiseNotFoundException(noiseId, userId);
        }
    }

    public List<NoiseDTO> getUserNoiseHistory(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        List<Noise> noises = noiseRepository.findAllByUser_UserId(userId);
        return noises.stream()
                .map(NoiseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private String generateFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "noise_result.jpg";
        }

        // 확장자 추출
        int dotIndex = originalFileName.lastIndexOf('.');
        String extension = dotIndex != -1 ? originalFileName.substring(dotIndex) : ".jpg";

        // 확장자 검증
        if (extension.length() > 5 || extension.length() <= 1) {
            extension = ".jpg";
        }

        // 파일명 정리
        String baseName = dotIndex != -1 ? originalFileName.substring(0, dotIndex) : originalFileName;
        baseName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_");

        if (baseName.length() > 15) {
            baseName = baseName.substring(0, 15);
        }

        return baseName + "_noise" + extension.toLowerCase();
    }

}