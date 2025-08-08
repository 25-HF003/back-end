package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.entity.Noise;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

        List<Noise> noiseRecords = noiseRepository.findAllByUser_UserId(userId);
        return noiseRecords.stream()
                .map(NoiseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}