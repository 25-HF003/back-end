package com.deeptruth.deeptruth.repository;

import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.entity.Watermark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatermarkRepository extends JpaRepository<Watermark, Long> {
    List<Watermark> findAllByUser(User user);
    int deleteByWatermarkIdAndUser(Long id, User user);

    Optional<Watermark> findByWatermarkIdAndUser(Long id, User user);

    Page<Watermark> findByUser_UserId(Long userId, Pageable pageable);

    Optional<Watermark> findFirstBySha256(String sha256);

    Optional<Watermark> findFirstByNormalizedSha256(String normalizedSha256);

    // pHash 근사: 가장 가까운 1건
    @Query(value = "SELECT * FROM watermark ORDER BY BIT_COUNT(phash ^ :phash) ASC LIMIT 1", nativeQuery = true)
    Watermark findNearestByPhash(@Param("phash") long phash);
}