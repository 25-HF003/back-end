package com.deeptruth.deeptruth.repository;

import com.deeptruth.deeptruth.entity.Noise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoiseRepository extends JpaRepository<Noise, Long> {
    List<Noise> findAllByUserId(Long userId);
    boolean existsByUserId(Long userId);
}