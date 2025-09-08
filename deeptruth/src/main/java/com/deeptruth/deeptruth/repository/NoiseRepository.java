package com.deeptruth.deeptruth.repository;

import com.deeptruth.deeptruth.entity.Noise;
import com.deeptruth.deeptruth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoiseRepository extends JpaRepository<Noise, Long> {
    List<Noise> findAllByUser_UserId(Long userId);
    Page<Noise> findByUser_UserId(Long userId, Pageable pageable);
    Optional<Noise> findByNoiseIdAndUser(Long noiseId, User user);
    int deleteByNoiseIdAndUser(Long noiseId, User user);


    boolean existsByUser_UserId(Long userId);

    // 삭제 메서드
    @Modifying
    @Query("DELETE FROM Noise n WHERE n.user = :user")
    int deleteByUser(@Param("user") User user);
}