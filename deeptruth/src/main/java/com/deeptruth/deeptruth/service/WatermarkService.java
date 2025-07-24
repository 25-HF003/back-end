package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.watermark.WatermarkDTO;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.entity.Watermark;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.repository.WatermarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WatermarkService {
    private final WatermarkRepository watermarkRepository;
    private final UserRepository userRepository;
    private final AmazonS3Service amazonS3Service;

    public List<WatermarkDTO> getAllResult(Long userId){
        User user = userRepository.findById(userId).orElseThrow();
        List<Watermark> results = watermarkRepository.findAllByUser(user);

        return results.stream()
                .map(WatermarkDTO::fromEntity)
                .collect(Collectors.toList());
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
