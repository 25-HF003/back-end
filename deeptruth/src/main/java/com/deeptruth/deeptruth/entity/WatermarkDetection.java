package com.deeptruth.deeptruth.entity;

import com.deeptruth.deeptruth.base.Enum.MatchMethod;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "watermarkDetection",
        indexes = {
                @Index(name = "idx_wmd_wm", columnList = "watermark_id"),
                @Index(name = "idx_wmd_user_created", columnList = "user_id, detected_at")
        }
)
public class WatermarkDetection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long watermarkDetectionId;

    @ManyToOne
    @JoinColumn(name = "watermark_id")
    private Watermark watermark;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 업로드 이미지로 계산한 해시들 (로그/분석용) */
    @Column(name = "uploaded_sha256", length = 64)
    private String uploadedSha256;

    @Column(name = "uploaded_normalized_sha256", length = 64)
    private String uploadedNormalizedSha256;

    @Column(name = "uploaded_phash")
    private Long uploadedPhash;

    /** 매칭 방식: SHA256 / NORMALIZED_SHA256 / PHASH / NONE */
    @Enumerated(EnumType.STRING)
    @Column(name = "match_method", length = 24, nullable = false)
    private MatchMethod matchMethod;

    /** pHash 근사매칭 시 해밍거리(정확/정규화 일치면 0 또는 null) */
    @Column(name = "phash_distance")
    private Integer phashDistance;

    /** Flask가 리턴한 비트 정확도(%) */
    @Column(name = "bit_accuracy")
    private Float bitAccuracy;

    /** 탐지 시점 */
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(nullable=false, updatable = false)
    private LocalDateTime watermarkDetectedAt;

    @Column
    private String taskId;

    @PrePersist
    protected void onCreate() {
        this.watermarkDetectedAt = LocalDateTime.now();
    }
}
