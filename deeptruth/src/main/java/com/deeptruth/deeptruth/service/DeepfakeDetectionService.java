package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.base.dto.deepfake.FlaskResponseDTO;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONUtil;
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

    public DeepfakeDetectionDTO uploadVideo(Long userId, MultipartFile multipartFile){
        User user = userRepository.findById(userId).orElseThrow();
        String filePath = amazonS3Service.uploadFile("deepfake", multipartFile);

        DeepfakeResult deepfakeResult = DeepfakeResult.FAKE;
        Float riskScore = 0.7F;


        DeepfakeDetection detection = DeepfakeDetection.builder()
                .user(user)
                .filePath(filePath)
                .result(deepfakeResult)
                .build();

        deepfakeDetectionRepository.save(detection);

        DeepfakeDetectionDTO dto = DeepfakeDetectionDTO.fromEntity(detection);

        return dto;
    }

    public DeepfakeDetectionDTO createDetection(Long userId, FlaskResponseDTO flaskResponseDTO){
        User user = userRepository.findById(userId).orElseThrow();

        DeepfakeDetection detection = DeepfakeDetection.builder()
                .user(user)
                .filePath(flaskResponseDTO.getImageUrl())
                .result(flaskResponseDTO.getResult())
                .averageConfidence(flaskResponseDTO.getAverageConfidence())
                .maxConfidence(flaskResponseDTO.getMaxConfidence())
                .build();

        deepfakeDetectionRepository.save(detection);

        return DeepfakeDetectionDTO.fromEntity(detection);
    }

    public String uploadBase64ImageToS3(String base64Image, Long userId) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Image);
        InputStream inputStream = new ByteArrayInputStream(decodedBytes);

        String key = "deepfake/" + userId + "/" + UUID.randomUUID() + ".jpg";
        String imageUrl = amazonS3Service.uploadBase64Image(inputStream, key);

        return imageUrl;
    }

    public List<DeepfakeDetectionDTO> getAllResult(Long userId){
        User user = userRepository.findById(userId).orElseThrow();
        List<DeepfakeDetection> results = deepfakeDetectionRepository.findAllByUser(user);

        return results.stream()
                .map(DeepfakeDetectionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public DeepfakeDetectionDTO getSingleResult(Long userId, Long id) {
        User user = userRepository.findById(userId).orElseThrow();
        DeepfakeDetection detection = deepfakeDetectionRepository.findByDeepfakeDetectionIdAndUser(id, user).orElseThrow();

        return DeepfakeDetectionDTO.fromEntity(detection);
    }

    public void deleteResult(Long userId, Long id){
        User user = userRepository.findById(userId).orElseThrow();

        deepfakeDetectionRepository.deleteByDeepfakeDetectionIdAndUser(id, user);
    }
}
