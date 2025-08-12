package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.watermark.WatermarkDTO;
import com.deeptruth.deeptruth.base.dto.watermark.WatermarkFlaskResponseDTO;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.entity.Watermark;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.repository.WatermarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WatermarkService {
    private final WatermarkRepository watermarkRepository;
    private final UserRepository userRepository;
    private final AmazonS3Service amazonS3Service;
      
    public WatermarkDTO createWatermark(Long userId, WatermarkFlaskResponseDTO dto){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (dto == null) {
            throw new InvalidWatermarkResponseException("dto is null");
        }
        if (dto.getWatermarkedFilePath() == null || dto.getWatermarkedFilePath().isBlank()) {
            throw new InvalidWatermarkResponseException("watermarkedFilePath is blank");
        }

        Watermark watermark = Watermark.builder()
                .user(user)
                .watermarkedFilePath(dto.getWatermarkedFilePath())
                .createdAt(LocalDateTime.now())
                .build();

        watermarkRepository.save(watermark);
        return WatermarkDTO.fromEntity(watermark);
    }

    public String uploadBase64ImageToS3(String base64Image, Long userId) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new ImageDecodingException("empty string");
        }

        final byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(base64Image);
        } catch (IllegalArgumentException e) {
            throw new ImageDecodingException("malformed base64");
        }

        try (InputStream inputStream = new ByteArrayInputStream(decodedBytes)) {
            String key = "watermark/marked/" + userId + "/" + UUID.randomUUID() + ".jpg";
            return amazonS3Service.uploadBase64Image(inputStream, key);
        } catch (Exception e) {
            // S3 업로드 실패 → 5xx
            throw new StorageException("failed to upload image to S3", e);
        }
    }
  
    public Page<WatermarkDTO> getAllResult(Long userId, Pageable pageable){
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return watermarkRepository.findByUser_UserId(userId, pageable)
                .map(WatermarkDTO::fromEntity);
    }

    public WatermarkDTO getSingleResult(Long userId, Long id){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Watermark mark = watermarkRepository.findByWatermarkIdAndUser(id, user).orElseThrow();

        return WatermarkDTO.fromEntity(mark);
    }

    public void deleteWatermark(Long userId, Long id){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        int deleted = watermarkRepository.deleteByWatermarkIdAndUser(id, user);
        if (deleted == 0) throw new WatermarkNotFoundException(id, userId);
    }
}
