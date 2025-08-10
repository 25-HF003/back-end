package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.watermark.WatermarkDTO;
import com.deeptruth.deeptruth.base.dto.watermark.WatermarkFlaskResponseDTO;
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
        User user = userRepository.findById(userId).orElseThrow();

        Watermark watermark = Watermark.builder()
                .user(user)
                .watermarkedFilePath(dto.getWatermarkedFilePath())
                .fileName(dto.getFilename())
                .createdAt(LocalDateTime.now())
                .build();

        watermarkRepository.save(watermark);
        return WatermarkDTO.fromEntity(watermark);
    }

    public String uploadBase64ImageToS3(String base64Image, Long userId) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Image);
        InputStream inputStream = new ByteArrayInputStream(decodedBytes);

        String key = "watermark/marked/" + userId + "/" + UUID.randomUUID() + ".jpg";
        String imageUrl = amazonS3Service.uploadBase64Image(inputStream, key);

        return imageUrl;
    }
  
    public Page<WatermarkDTO> getAllResult(Long userId, Pageable pageable){
        return watermarkRepository.findByUser_UserId(userId, pageable)
                .map(WatermarkDTO::fromEntity);
    }

    public WatermarkDTO getSingleResult(Long userId, Long id){
        User user = userRepository.findById(userId).orElseThrow();
        Watermark mark = watermarkRepository.findByWatermarkIdAndUser(id, user).orElseThrow();

        return WatermarkDTO.fromEntity(mark);
    }

    public void deleteWatermark(Long userId, Long id){
        User user = userRepository.findById(userId).orElseThrow();
        watermarkRepository.deleteByWatermarkIdAndUser(id, user);
    }
}
