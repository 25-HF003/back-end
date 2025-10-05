package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.websocket.DeepfakeRequestMessage;
import com.deeptruth.deeptruth.base.dto.deepfake.BulletDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionListDTO;
import com.deeptruth.deeptruth.base.dto.websocket.TaskAcceptedDTO;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.config.RabbitConfig;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

    private final ActiveTaskService activeTaskService;
    private final RabbitTemplate rabbitTemplate;


    @org.springframework.beans.factory.annotation.Value("${app.upload.prefix:uploads/deepfake}")
    private String uploadPrefix;

    public TaskAcceptedDTO createDetectionAsync(Long userId, MultipartFile file, Map<String,String> form) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (file == null || file.isEmpty()) throw new FileEmptyException();

        String ct = Optional.ofNullable(file.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        if (!(ct.startsWith("image/") || ct.startsWith("video/") || MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(ct))) {
            throw new UnsupportedMediaTypeException(ct);
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
        String taskId = form.getOrDefault("taskId", UUID.randomUUID().toString());
        long size = file.getSize();

        // 기준: 10MB 이하 & image/* 는 메시지로 직접, 그 외는 S3로
        final long INLINE_LIMIT = 10L * 1024 * 1024;

        // Task 등록(PENDING)
        activeTaskService.registerTask(user.getLoginId(), taskId);

        // 메시지 Publish
        DeepfakeRequestMessage.DeepfakeRequestMessageBuilder builder = DeepfakeRequestMessage.builder()
                .taskId(taskId)
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .originalFilename(filename)
                .contentType(ct)
                .mode(form.get("mode"))
                .detector(form.get("detector"))
                .useTta(form.get("use_tta"))
                .useIllum(form.get("use_illum"))
                .minFace(form.get("min_face"))
                .sampleCount(form.get("sample_count"))
                .smoothWindow(form.get("smooth_window"));

        try {
            if (ct.startsWith("image/") && size <= INLINE_LIMIT) {
                // 경로 A: 작은 이미지 → 메시지로 바로
                builder.fileBytes(file.getBytes());
            } else {
                // 경로 B: 큰 이미지/영상 → S3 업로드 후 키만 전달
                String key = uploadPrefix + "/" + userId + "/" + taskId + "/" + filename;
                try (InputStream in = file.getInputStream()) {
                    amazonS3Service.uploadBinary(in, key, ct);
                }
                builder.s3Key(key);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to prepare upload", e);
        }

        // Task 등록(PENDING)
        activeTaskService.registerTask(user.getLoginId(), taskId);

        // 메시지 발행 (오버로드 모호성 방지: 3-인자 호출)
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.REQ_ROUTING, builder.build());

        return new TaskAcceptedDTO(taskId);
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
