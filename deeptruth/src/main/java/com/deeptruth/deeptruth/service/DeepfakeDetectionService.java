package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.FlaskResponseDTO;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DeepfakeDetectionService {

    private final DeepfakeDetectionRepository deepfakeDetectionRepository;

    private final UserRepository userRepository;

    private final AmazonS3Service amazonS3Service;

    public DeepfakeDetectionDTO createDetection(Long userId, FlaskResponseDTO dto){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (dto == null) throw new InvalidDetectionResponseException("response is null");
        if (dto.getResult() == null) throw new InvalidDetectionResponseException("result is null");
        if (dto.getImageUrl() == null || dto.getImageUrl().isBlank())
            throw new InvalidDetectionResponseException("imageUrl is blank");
        if (dto.getAverageConfidence() != null &&
                (dto.getAverageConfidence() < 0 || dto.getAverageConfidence() > 1))
            throw new InvalidDetectionResponseException("averageConfidence out of [0,1]");
        if (dto.getMaxConfidence() != null &&
                (dto.getMaxConfidence() < 0 || dto.getMaxConfidence() > 1))
            throw new InvalidDetectionResponseException("maxConfidence out of [0,1]");


        DeepfakeDetection detection = DeepfakeDetection.builder()
                .user(user)
                .filePath(dto.getImageUrl())
                .result(dto.getResult())
                .averageConfidence(dto.getAverageConfidence())
                .maxConfidence(dto.getMaxConfidence())
                .build();

        deepfakeDetectionRepository.save(detection);
        return DeepfakeDetectionDTO.fromEntity(detection);
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

    public Page<DeepfakeDetectionDTO> getAllResult(Long userId, Pageable pageable){
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return deepfakeDetectionRepository.findByUser_UserId(userId, pageable)
                .map(DeepfakeDetectionDTO::fromEntity);
    }

    public DeepfakeDetectionDTO getSingleResult(Long userId, Long id) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        DeepfakeDetection detection = deepfakeDetectionRepository
                .findByDeepfakeDetectionIdAndUser(id, user)
                .orElseThrow(() -> new DetectionNotFoundException(id, userId));

        return DeepfakeDetectionDTO.fromEntity(detection);
    }

    public void deleteResult(Long userId, Long id){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        int deleted = deepfakeDetectionRepository.deleteByDeepfakeDetectionIdAndUser(id, user);
        if (deleted == 0) {
            throw new DetectionNotFoundException(id, userId);
        }
    }
}
