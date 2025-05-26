package com.deeptruth.deeptruth.repository;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeepfakeDetectionRepository extends JpaRepository<DeepfakeDetection, Long> {
    List<DeepfakeDetectionDTO> findAllAsDTO();
    List<DeepfakeDetection> findByUser(User user);
    void deleteById(Long id);
}
