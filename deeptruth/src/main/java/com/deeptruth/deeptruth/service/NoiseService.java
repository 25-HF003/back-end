package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.noise.NoiseCreateRequestDTO;
import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.entity.Noise;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoiseService {

    private final NoiseRepository noiseRepository;
    private final UserRepository userRepository;

    public List<NoiseDTO> getUserNoiseHistory(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자가 존재하지 않습니다.");
        }

        List<Noise> noises = noiseRepository.findAllByUser_UserId(userId);
        return noises.stream()
                .map(NoiseDTO::fromEntity)
                .collect(Collectors.toList());
    }


    @Transactional
    public NoiseDTO createNoise(Long userId, NoiseCreateRequestDTO request) {
        // 요청 데이터 검증
        request.validate();

        // User 엔티티 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 처리된 파일 경로 생성
        String processedFilePath = generateProcessedFilePath(request.getOriginalFilePath());

        // Noise 엔티티 생성
        Noise noise = Noise.builder()
                .user(user)
                .originalFilePath(request.getOriginalFilePath())
                .processedFilePath(processedFilePath)
                .epsilon(request.getEpsilon())
                .build();

        // DB 저장
        Noise savedNoise = noiseRepository.save(noise);

        // DTO 변환 후 반환
        return NoiseDTO.fromEntity(savedNoise);
    }

    // 임시 구현
    private String generateProcessedFilePath(String originalFilePath) {
        return originalFilePath.replace("/original/", "/processed/")
                .replace(".jpg", "_noised.jpg")
                .replace(".png", "_noised.png");
    }

}