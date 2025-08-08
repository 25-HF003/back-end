package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.entity.Noise;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoiseService {

    private final NoiseRepository noiseRepository;

    public List<NoiseDTO> getUserNoiseHistory(Long userId) {
        List<Noise> noiseRecords = noiseRepository.findAllByUserId(userId);
        return noiseRecords.stream()
                .map(NoiseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}