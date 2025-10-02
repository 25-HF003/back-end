package com.deeptruth.deeptruth.worker;

import com.deeptruth.deeptruth.base.Enum.DeepfakeDetector;
import com.deeptruth.deeptruth.base.Enum.DeepfakeMode;
import com.deeptruth.deeptruth.base.dto.websocket.DeepfakeRequestMessage;
import com.deeptruth.deeptruth.base.dto.deepfake.BulletDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionListDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.FlaskResponseDTO;
import com.deeptruth.deeptruth.base.dto.websocket.TaskEvent;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.config.RabbitConfig;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.service.ActiveTaskService;
import com.deeptruth.deeptruth.service.AmazonS3Service;
import com.deeptruth.deeptruth.service.DeepfakeViewAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeepfakeWorker {

    private final SimpMessagingTemplate ws;
    private final DeepfakeDetectionRepository repo;
    private final UserRepository userRepository;
    private final DeepfakeViewAssembler assembler;
    private final WebClient webClient;
    private final ActiveTaskService activeTaskService;
    private final DeepfakeDetectionRepository deepfakeDetectionRepository;
    private final AmazonS3Service amazonS3Service;


    @org.springframework.beans.factory.annotation.Value("${flask.deepfakeServer.url}")
    private String flaskServerUrl;

    @RabbitListener(queues = RabbitConfig.REQ_QUEUE)
    public void onMessage(DeepfakeRequestMessage msg) {
        final String taskId = msg.getTaskId();
        final String loginId = msg.getLoginId();

        try {
            sendProgress(loginId, taskId, 5);

            final String filename = Optional.ofNullable(msg.getOriginalFilename()).orElse("upload.bin");
            final String contentType = Optional.ofNullable(msg.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            // 1) 파일 파트 구성
            byte[] bodyBytes;
            if (msg.getFileBytes() != null && msg.getFileBytes().length > 0) {
                bodyBytes = msg.getFileBytes();
            } else if (msg.getS3Key() != null) {
                try (InputStream in = amazonS3Service.openStream(msg.getS3Key())) {
                    bodyBytes = in.readAllBytes(); // Java 11+
                }
            } else {
                throw new IllegalStateException("No input provided (fileBytes or s3Key missing)");
            }

            MultipartBodyBuilder mb = new MultipartBodyBuilder();
            ByteArrayResource res = new ByteArrayResource(bodyBytes) {
                @Override public String getFilename() { return filename; }
            };
            mb.part("file", res)
                    .filename(filename)
                    .contentType(MediaType.parseMediaType(contentType));

            // pass-through 옵션
            pass(mb, "taskId", taskId);
            pass(mb, "loginId", loginId);
            pass(mb, "mode", msg.getMode());
            pass(mb, "detector", msg.getDetector());
            pass(mb, "use_tta", msg.getUseTta());
            pass(mb, "use_illum", msg.getUseIllum());
            pass(mb, "min_face", msg.getMinFace());
            pass(mb, "sample_count", msg.getSampleCount());
            pass(mb, "smooth_window", msg.getSmoothWindow());

            sendProgress(loginId, taskId, 10);

            // 2) Flask 호출
            FlaskResponseDTO flask = webClient.post()
                    .uri(flaskServerUrl + "/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(mb.build()))
                    .retrieve()
                    .bodyToMono(FlaskResponseDTO.class)
                    .block();

            if (flask == null) throw new RuntimeException("Flask response is null");

            // 3) 결과 이미지 S3 저장
            if (flask.getBase64Url() != null && !flask.getBase64Url().isEmpty()) {
                String uploaded = uploadBase64ImageToS3(flask.getBase64Url(), msg.getUserId());
                flask.setImageUrl(uploaded);
            }

            sendProgress(loginId, taskId, 70);

            // 4) DB 저장 + WS 완료
            User user = userRepository.findById(msg.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + msg.getUserId()));

            DeepfakeDetection entity = mapToEntity(user, flask); // 네 기존 메서드 재사용
            List<BulletDTO> stability = assembler.makeStabilityBullets(entity);
            List<BulletDTO> speed = assembler.makeSpeedBullets(entity);
            if (stability == null || speed == null) throw new RuntimeException("bullet assembling failed");
            entity.setStabilityScore(DeepfakeViewAssembler.meanScore(stability));
            entity.setSpeedScore(DeepfakeViewAssembler.meanScore(speed));
            repo.save(entity);

            sendProgress(loginId, taskId, 100);

            ws.convertAndSendToUser(loginId, "/queue/tasks/" + taskId,
                    TaskEvent.done(taskId, DeepfakeDetectionDTO.fromEntity(entity, stability, speed)));

        } catch (Exception e) {
            log.error("Deepfake task failed: taskId={}, err={}", taskId, e.getMessage(), e);
            ws.convertAndSendToUser(loginId, "/queue/tasks/" + taskId, TaskEvent.error(taskId, "AI_PROCESS_FAILED"));
            throw new AmqpRejectAndDontRequeueException("fail", e);
        } finally {
            activeTaskService.registerTask(loginId, taskId);
        }
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
    private void pass(MultipartBodyBuilder mb, String k, String v) {
        if (v != null) mb.part(k, v, MediaType.TEXT_PLAIN);
    }
    private void sendProgress(String loginId, String taskId, int p) {
        ws.convertAndSendToUser(loginId, "/queue/tasks/" + taskId, TaskEvent.progress(taskId, p));
    }

    private DeepfakeDetection mapToEntity(User user, FlaskResponseDTO dto) {
        if (dto == null) throw new DataMappingException("flask response is null");
        if (dto.getTaskId() == null || dto.getImageUrl() == null || dto.getResult() == null) {
            throw new DataMappingException("required fields missing in flask response");
        }
        DeepfakeDetection detection = new DeepfakeDetection();
        detection.setUser(user);
        detection.setTaskId(dto.getTaskId());
        detection.setFilePath(dto.getImageUrl());
        detection.setResult(dto.getResult());

        // 판정/점수
        detection.setScoreWeighted(dto.getScoreWeighted());
        detection.setThresholdTau(dto.getThresholdTau());
        detection.setFrameVoteRatio(dto.getFrameVoteRatio());

        // 통계
        detection.setAverageConfidence(dto.getAverageConfidence());
        detection.setMedianConfidence(dto.getMedianConfidence());
        detection.setMaxConfidence(dto.getMaxConfidence());
        detection.setVarianceConfidence(dto.getVarianceConfidence());

        // 처리량/시간
        detection.setFramesProcessed(dto.getFramesProcessed());
        detection.setProcessingTimeSec(dto.getProcessingTimeSec());

        // 속도(SLA 에코)
        if (dto.getSpeed() != null) {
            detection.setMsPerSample(dto.getSpeed().getMsPerSample());
            detection.setTargetFpsUsed(dto.getSpeed().getTargetFps());
            detection.setMaxLatencyMsUsed(dto.getSpeed().getMaxLatencyMs());
            detection.setSpeedOk(dto.getSpeed().getSpeedOk());
            detection.setFpsProcessed(dto.getSpeed().getFpsProcessed());
        }

        // 안정성 원시
        if (dto.getStabilityEvidence() != null) {
            detection.setTemporalDeltaMean(dto.getStabilityEvidence().getTemporalDeltaMean());
            detection.setTemporalDeltaStd(dto.getStabilityEvidence().getTemporalDeltaStd());
            detection.setTtaMean(dto.getStabilityEvidence().getTtaMean());
            detection.setTtaStd(dto.getStabilityEvidence().getTtaStd());
        }

        // 실행 환경/프로비넌스
        if (dto.getMode() != null) {
            try {
                detection.setMode(DeepfakeMode.valueOf(dto.getMode().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new InvalidEnumValueException("mode", dto.getMode());
            }
        }
        if (dto.getDetector() != null) {
            try {
                detection.setDetector(DeepfakeDetector.valueOf(dto.getDetector().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new InvalidEnumValueException("detector", dto.getDetector());
            }
        }
        detection.setUseTta(dto.getUseTta());
        detection.setUseIllum(dto.getUseIllum());
        detection.setMinFace(dto.getMinFace());
        detection.setSampleCount(dto.getSampleCount());
        detection.setSmoothWindow(dto.getSmoothWindow());

        // 히트맵
        if (dto.getTimeseries() != null && dto.getTimeseries().getPerFrameConf() != null) {
            String json = "{\"per_frame_conf\":" + toJsonArray(dto.getTimeseries().getPerFrameConf())
                    + ",\"vmin\":" + numOrNull(dto.getTimeseries().getVmin())
                    + ",\"vmax\":" + numOrNull(dto.getTimeseries().getVmax()) + "}";
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

}