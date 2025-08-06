package com.deeptruth.deeptruth.repository;

import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.entity.Watermark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatermarkRepository extends JpaRepository<Watermark, Long> {
    List<Watermark> findAllByUser(User user);
    void deleteByWatermarkIdAndUser(Long id, User user);

    Optional<Watermark> findByWatermarkIdAndUser(Long id, User user);

    Page<Watermark> findByUser_UserId(Long userId, Pageable pageable);

}