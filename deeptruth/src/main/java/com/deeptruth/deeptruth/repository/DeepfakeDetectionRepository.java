package com.deeptruth.deeptruth.repository;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeepfakeDetectionRepository extends JpaRepository<DeepfakeDetection, Long> {
    void deleteByDeepfakeDetectionId(Long id);
    Optional<DeepfakeDetection> findByDeepfakeDetectionIdAndUser(Long id, User user);
    void deleteByDeepfakeDetectionIdAndUser(Long id, User user);
    List<DeepfakeDetection> findAllByUser(User user);

}
