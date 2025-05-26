package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeepfakeDetectionService {

    private final DeepfakeDetectionRepository deepfakeDetectionRepository;

    private final UserRepository userRepository;

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
