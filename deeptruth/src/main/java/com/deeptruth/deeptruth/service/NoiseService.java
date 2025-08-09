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

    // 노이즈 개별 조회 (권한 검증 포함)
    @Transactional(readOnly = true)
    public NoiseDTO getNoiseById(Long userId, Long noiseId) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 노이즈 조회
        Noise noise = noiseRepository.findById(noiseId)
                .orElseThrow(() -> new IllegalArgumentException("노이즈를 찾을 수 없습니다."));

        // 권한 검증 (본인의 노이즈인지 확인)
        if (!noise.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return NoiseDTO.fromEntity(noise);
    }

    @Transactional
    public void deleteNoise(Long userId, Long noiseId) {
        // 1. 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 2. 노이즈 조회
        Noise noise = noiseRepository.findById(noiseId)
                .orElseThrow(() -> new IllegalArgumentException("노이즈를 찾을 수 없습니다."));

        // 3. 권한 확인 (본인의 노이즈인지)
        if (!noise.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // 4. 삭제 실행
        noiseRepository.delete(noise);
    }
}