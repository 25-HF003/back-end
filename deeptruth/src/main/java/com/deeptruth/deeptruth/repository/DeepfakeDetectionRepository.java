package com.deeptruth.deeptruth.repository;

import com.deeptruth.deeptruth.base.dto.deepfake.DeepfakeDetectionDTO;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeepfakeDetectionRepository extends JpaRepository<DeepfakeDetection, Long> {
    void deleteByDeepfakeDetectionId(Long id);
    Optional<DeepfakeDetection> findByDeepfakeDetectionIdAndUser(Long id, User user);
    int deleteByDeepfakeDetectionIdAndUser(Long id, User user);
    List<DeepfakeDetection> findAllByUser(User user);
    Page<DeepfakeDetection> findByUser_UserId(Long userId, Pageable pageable);

    // 삭제 메서드
    @Modifying
    @Query("DELETE FROM DeepfakeDetection d WHERE d.user = :user")
    int deleteByUser(@Param("user") User user);
}
