package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.watermarkDetection.DetectResultDTO;
import com.deeptruth.deeptruth.base.dto.watermarkDetection.WatermarkDetectionFlaskResponseDTO;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.entity.Watermark;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.repository.WatermarkRepository;
import com.deeptruth.deeptruth.util.ImageHashUtils;
import com.deeptruth.deeptruth.util.ImageNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatermarkDetectionService {
    private final WatermarkRepository watermarkRepository;
    private final WebClient webClient;
    private final UserRepository userRepository;

    @Value("${flask.watermark-server.url}")
    private String flaskBaseUrl;

    // pHash 임계값(64bit): 실데이터로 튜닝 요망 (10~12 사이 권장)
    private static final int PHASH_THRESHOLD = 10;


    public DetectResultDTO detect(Long userId, MultipartFile file, String taskId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (file == null || file.isEmpty()) throw new FileEmptyException();
        String originalFilename = (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank())
                ? "upload.png" : file.getOriginalFilename();
        if (!originalFilename.contains(".")) throw new InvalidFilenameException(originalFilename);
        if (taskId == null || taskId.isBlank()) taskId = UUID.randomUUID().toString();

        final byte[] uploaded;
        try {
            uploaded = file.getBytes();
        } catch (IOException e) {
            throw new DataMappingException("업로드 파일을 읽을 수 없습니다.");
        }

        // 1) sha256 정확 매칭
        var sha = ImageHashUtils.sha256(uploaded);
        var hit = watermarkRepository.findFirstBySha256(sha);

        String matchMethod = null;
        Integer phashDistance = null;
        Watermark matched = null;

        // 2) sha256 정확 매칭
        if (hit.isPresent()) {
            matched = hit.get();
            matchMethod = "SHA256";
        }
        else {
            // 3) normalized sha256
            byte[] normalized = ImageNormalizer.normalizeToPng(uploaded);
            var nsha = ImageHashUtils.sha256(normalized);
            hit = watermarkRepository.findFirstByNormalizedSha256(nsha);
            if (hit.isPresent()) {
                matched = hit.get();
                matchMethod = "NORMALIZED_SHA256";
            } else {
                // 4) pHash 근사 매칭
                if (hit.isEmpty()) {
                    long p = ImageHashUtils.pHash(uploaded);
                    Watermark near = watermarkRepository.findNearestByPhash(p);
                    if (near == null) {
                        throw new ArtifactNotFoundException("유사한 워터마크 아티팩트가 없습니다.");
                    }
                    int dist = ImageHashUtils.hammingDistance(p, near.getPhash());
                    if (dist > PHASH_THRESHOLD) {
                        throw new SimilarityThresholdExceededException(dist, PHASH_THRESHOLD);
                    }
                    matched = near;
                    matchMethod = "PHASH";
                    phashDistance = dist;
                }
            }
        }


        // DB에서 message 확보
        String message = matched.getMessage();
        if (message == null || message.isBlank()) {
            throw new DataMappingException("매칭된 워터마크에 저장된 메시지가 없습니다.");
        }

        // 4) Flask /watermark-detection 호출 (image + message)
        ByteArrayResource imagePart = new ByteArrayResource(uploaded) {
            @Override public String getFilename() { return file.getOriginalFilename(); }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("image", imagePart);
        form.add("message", message);
        form.add("taskId", taskId);

        WatermarkDetectionFlaskResponseDTO flask;
        try {
            flask = webClient.post()
                    .uri(flaskBaseUrl + "/watermark-detection")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(form))
                    .exchangeToMono(res -> {
                        if (res.statusCode().is2xxSuccessful()) {
                            return res.bodyToMono(WatermarkDetectionFlaskResponseDTO.class);
                        } else {
                            return res.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new IllegalStateException(
                                            "Flask 호출 실패: " + res.statusCode().value() + " - " + body
                                    )));
                        }
                    })
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            throw new ExternalServiceException("Flask HTTP error: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            throw new ExternalServiceException("Flask request failed: " + e.getMessage());
        } catch (Exception e) {
            throw new ExternalServiceException("Flask invocation failed");
        }

        if (flask == null) {
            throw new ExternalServiceException("Flask 응답이 비어 있습니다.");
        }
        if (flask.getBit_accuracy() == null || flask.getDetected_at() == null) {
            throw new DataMappingException("Flask 응답 필드 누락(bit_accuracy/detected_at)");
        }


        // 5) 최종 응답 DTO 구성
        return DetectResultDTO.builder()
                .artifactId(matched.getArtifactId())
                .matchMethod(matchMethod)
                .phashDistance(phashDistance)
                .bitAccuracy(flask.getBit_accuracy())
                .detectedAt(flask.getDetected_at())
                .uploadedImageBase64(flask.getImage_base64()) // 임계 미달 시에만 세팅됨
                .basename(flask.getBasename())
                .taskId(taskId)
                .build();
    }
}
