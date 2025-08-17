package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.watermarkDetection.DetectResultDTO;
import com.deeptruth.deeptruth.base.dto.watermarkDetection.WatermarkDetectionFlaskResponseDTO;
import com.deeptruth.deeptruth.entity.Watermark;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatermarkDetectionService {
    private final WatermarkRepository watermarkRepository;
    private final WebClient webClient;

    @Value("${flask.watermark-server.url}")
    private String flaskBaseUrl;

    // pHash 임계값(64bit): 실데이터로 튜닝 요망 (10~12 사이 권장)
    private static final int PHASH_THRESHOLD = 10;


    public DetectResultDTO detect(Long userId, MultipartFile file) {
        final byte[] uploaded;
        try {
            uploaded = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("업로드 파일 읽기 실패", e);
        }

        // 1) sha256 정확 매칭
        var sha = ImageHashUtils.sha256(uploaded);
        var hit = watermarkRepository.findFirstBySha256(sha);

        String matchMethod = null;
        Integer phashDistance = null;
        Watermark matched;

        // 2) normalized sha256
        if (hit.isEmpty()) {
            byte[] normalized = ImageNormalizer.normalizeToPng(uploaded);
            var nsha = ImageHashUtils.sha256(normalized);
            hit = watermarkRepository.findFirstByNormalizedSha256(nsha);
            if (hit.isPresent()) {
                matchMethod = "NORMALIZED_SHA256";
            }
        } else {
            matchMethod = "SHA256";
        }

        // 3) pHash 근사 매칭
        if (hit.isEmpty()) {
            long p = ImageHashUtils.pHash(uploaded);
            Watermark near = watermarkRepository.findNearestByPhash(p);
            if (near == null) {
                throw new IllegalStateException("유사한 아티팩트가 없습니다.");
            }
            int dist = ImageHashUtils.hammingDistance(p, near.getPhash());
            if (dist > PHASH_THRESHOLD) {
                throw new IllegalStateException("유사도가 임계값을 넘습니다. dist=" + dist);
            }
            matched = near;
            matchMethod = "PHASH";
            phashDistance = dist;
        } else {
            matched = hit.get();
        }

        // DB에서 message 확보
        String message = matched.getMessage();

        // 4) Flask /watermark-detection 호출 (image + message)
        ByteArrayResource imagePart = new ByteArrayResource(uploaded) {
            @Override public String getFilename() { return file.getOriginalFilename(); }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("image", imagePart);
        form.add("message", message);

        WatermarkDetectionFlaskResponseDTO flask = webClient.post()
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

        if (flask == null) {
            throw new IllegalStateException("Flask 응답이 비어 있습니다.");
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
                .build();
    }
}
