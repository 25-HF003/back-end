package com.deeptruth.deeptruth.repository;

import com.deeptruth.deeptruth.entity.Noise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoiseRepository extends JpaRepository<Noise, Long> {
    List<Noise> findAllByUser_UserId(Long userId);

    boolean existsByUser_UserId(Long userId);
}