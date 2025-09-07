package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.DeepfakeDetector;
import com.deeptruth.deeptruth.base.Enum.DeepfakeMode;
import com.deeptruth.deeptruth.base.dto.deepfake.BulletDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionListDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.FlaskResponseDTO;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class DeepfakeDetectionService {

    private final DeepfakeDetectionRepository deepfakeDetectionRepository;
    private final UserRepository userRepository;
    private final AmazonS3Service amazonS3Service;
    private final DeepfakeViewAssembler assembler;
    private final WebClient webClient;
    @Value("${flask.deepfakeServer.url}")
    private String flaskServerUrl;

    public DeepfakeDetectionDTO createDetection(Long userId,
                                                MultipartFile file,
                                                Map<String, String> form){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (file == null || file.isEmpty()) throw new FileEmptyException();

        String contentType = file.getContentType();
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        if (!(contentType.startsWith("image/") || contentType.startsWith("video/")
                || MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType))) {
            throw new UnsupportedMediaTypeException(contentType);
        }

        String taskId = form.getOrDefault("taskId", UUID.randomUUID().toString());

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("file", file.getResource())
                .filename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.mp4")
                .contentType(file.getContentType() != null ? MediaType.parseMediaType(file.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM);

        passThrough(mb, "taskId", taskId);
        passThrough(mb, "mode", form.get("mode"));
        passThrough(mb, "detector", form.get("detector"));
        passThrough(mb, "use_tta", form.get("use_tta"));
        passThrough(mb, "use_illum", form.get("use_illum"));
        passThrough(mb, "min_face", form.get("min_face"));
        passThrough(mb, "sample_count", form.get("sample_count"));
        passThrough(mb, "smooth_window", form.get("smooth_window"));
//        passThrough(mb, "target_fps", form.get("target_fps"));
//        passThrough(mb, "max_latency_ms", form.get("max_latency_ms"));

        FlaskResponseDTO flaskResult;
        try {
            flaskResult = webClient.post()
                    .uri(flaskServerUrl + "/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(mb.build()))
                    .retrieve()
                    .bodyToMono(FlaskResponseDTO.class)
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            // HTTP 응답은 왔지만 4xx/5xx
            throw new ExternalServiceException("Flask HTTP error: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // 연결 실패/타임아웃 등
            throw new ExternalServiceException("Flask request failed: " + e.getMessage());
        }  catch (Exception e) {
            throw new ExternalServiceException("Flask invocation failed");
        }


        if (flaskResult == null) {
            throw new ExternalServiceException("Flask response is null");
        }

        String base64Image = flaskResult.getBase64Url();

        if (base64Image != null && !base64Image.isEmpty()) {
            flaskResult.setImageUrl(uploadBase64ImageToS3(base64Image, user.getUserId()));
        }

        DeepfakeDetection entity = mapToEntity(user, flaskResult);
        List<BulletDTO> stability = assembler.makeStabilityBullets(entity);
        List<BulletDTO> speed     = assembler.makeSpeedBullets(entity);
        if (stability == null || speed == null) {
            throw new DataMappingException("bullet assembling failed");
        }
        entity.setStabilityScore(DeepfakeViewAssembler.meanScore(stability));
        entity.setSpeedScore(DeepfakeViewAssembler.meanScore(speed));
        deepfakeDetectionRepository.save(entity);

        return DeepfakeDetectionDTO.fromEntity(entity,stability, speed);
    }

    public String uploadBase64ImageToS3(String base64Image, Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        final byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(base64Image);
        } catch (IllegalArgumentException e) {
            throw new ImageDecodingException("invalid base64");
        }

        try (InputStream inputStream = new ByteArrayInputStream(decodedBytes)) {
            String key = "deepfake/" + userId + "/" + UUID.randomUUID() + ".jpg";
            return amazonS3Service.uploadBase64Image(inputStream, key);
        } catch (Exception e) {
            throw new StorageException("failed to upload image to S3", e);
        }
    }

    private static void passThrough(MultipartBodyBuilder mb, String key, String val) {
        if (val != null) {
            mb.part(key, val, MediaType.TEXT_PLAIN);
        }
    }

    private DeepfakeDetection mapToEntity(User user, FlaskResponseDTO flaskResponseDTO) {
        if (flaskResponseDTO == null) throw new DataMappingException("flask response is null");
        if (flaskResponseDTO.getTaskId() == null || flaskResponseDTO.getImageUrl() == null || flaskResponseDTO.getResult() == null) {
            throw new DataMappingException("required fields missing in flask response");
        }
        DeepfakeDetection detection = new DeepfakeDetection();
        detection.setUser(user);
        detection.setTaskId(flaskResponseDTO.getTaskId());
        detection.setFilePath(flaskResponseDTO.getImageUrl());
        detection.setResult(flaskResponseDTO.getResult());

        // 판정/점수
        detection.setScoreWeighted(flaskResponseDTO.getScoreWeighted());
        detection.setThresholdTau(flaskResponseDTO.getThresholdTau());
        detection.setFrameVoteRatio(flaskResponseDTO.getFrameVoteRatio());

        // 통계
        detection.setAverageConfidence(flaskResponseDTO.getAverageConfidence());
        detection.setMedianConfidence(flaskResponseDTO.getMedianConfidence());
        detection.setMaxConfidence(flaskResponseDTO.getMaxConfidence());
        detection.setVarianceConfidence(flaskResponseDTO.getVarianceConfidence());

        // 처리량/시간
        detection.setFramesProcessed(flaskResponseDTO.getFramesProcessed());
        detection.setProcessingTimeSec(flaskResponseDTO.getProcessingTimeSec());

        // 속도(SLA 에코)
        if (flaskResponseDTO.getSpeed() != null) {
            detection.setMsPerSample(flaskResponseDTO.getSpeed().getMsPerSample());
            detection.setTargetFpsUsed(flaskResponseDTO.getSpeed().getTargetFps());
            detection.setMaxLatencyMsUsed(flaskResponseDTO.getSpeed().getMaxLatencyMs());
            detection.setSpeedOk(flaskResponseDTO.getSpeed().getSpeedOk());
            detection.setFpsProcessed(flaskResponseDTO.getSpeed().getFpsProcessed());
        }

        // 안정성 원시
        if (flaskResponseDTO.getStabilityEvidence() != null) {
            detection.setTemporalDeltaMean(flaskResponseDTO.getStabilityEvidence().getTemporalDeltaMean());
            detection.setTemporalDeltaStd(flaskResponseDTO.getStabilityEvidence().getTemporalDeltaStd());
            detection.setTtaMean(flaskResponseDTO.getStabilityEvidence().getTtaMean());
            detection.setTtaStd(flaskResponseDTO.getStabilityEvidence().getTtaStd());
        }

        // 실행 환경/프로비넌스
        if (flaskResponseDTO.getMode() != null) {
            try {
                detection.setMode(DeepfakeMode.valueOf(flaskResponseDTO.getMode().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new InvalidEnumValueException("mode", flaskResponseDTO.getMode());
            }
        }
        if (flaskResponseDTO.getDetector() != null) {
            try {
                detection.setDetector(DeepfakeDetector.valueOf(flaskResponseDTO.getDetector().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new InvalidEnumValueException("detector", flaskResponseDTO.getDetector());
            }
        }
        detection.setUseTta(flaskResponseDTO.getUseTta());
        detection.setUseIllum(flaskResponseDTO.getUseIllum());
        detection.setMinFace(flaskResponseDTO.getMinFace());
        detection.setSampleCount(flaskResponseDTO.getSampleCount());
        detection.setSmoothWindow(flaskResponseDTO.getSmoothWindow());

        // 히트맵
        if (flaskResponseDTO.getTimeseries() != null && flaskResponseDTO.getTimeseries().getPerFrameConf() != null) {
            String json = "{\"per_frame_conf\":" + toJsonArray(flaskResponseDTO.getTimeseries().getPerFrameConf())
                    + ",\"vmin\":" + numOrNull(flaskResponseDTO.getTimeseries().getVmin())
                    + ",\"vmax\":" + numOrNull(flaskResponseDTO.getTimeseries().getVmax()) + "}";
            detection.setTimeseriesJson(json);
        }

        return detection;
    }

    private static String toJsonArray(List<Float> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<list.size();i++){
            if (i>0) sb.append(',');
            Float v = list.get(i);
            sb.append(v==null?"null":String.valueOf(v));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String numOrNull(Number n){ return n==null?"null":String.valueOf(n); }

    public Page<DeepfakeDetectionListDTO> getAllResult(Long userId, Pageable pageable){
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (pageable == null) throw new DataMappingException("pageable is null");
        return deepfakeDetectionRepository.findByUser_UserId(userId, pageable)
                .map(DeepfakeDetectionListDTO::fromEntity);
    }

    public DeepfakeDetectionDTO getSingleResult(Long userId, Long id) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        DeepfakeDetection entity = deepfakeDetectionRepository
                .findByDeepfakeDetectionIdAndUser(id, user)
                .orElseThrow(() -> new DetectionNotFoundException(id, userId));

        List<BulletDTO> stability = assembler.makeStabilityBullets(entity);
        List<BulletDTO> speed     = assembler.makeSpeedBullets(entity);

        if (stability == null || speed == null) {
            throw new DataCorruptionException("Bullet assembling failed: null list");
        }

        return DeepfakeDetectionDTO.fromEntity(entity, stability, speed);
    }

    public void deleteResult(Long userId, Long id){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        DeepfakeDetection entity = deepfakeDetectionRepository
                .findByDeepfakeDetectionIdAndUser(id, user)
                .orElseThrow(() -> new DetectionNotFoundException(id, userId));

        int deleted = deepfakeDetectionRepository.deleteByDeepfakeDetectionIdAndUser(id, user);
        if (deleted == 0) {
            throw new DetectionNotFoundException(id, userId);
        }
    }
}
