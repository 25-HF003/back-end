package com.deeptruth.deeptruth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "watermark",
        indexes = {
                @Index(name = "idx_wm_sha256", columnList = "sha256"),
                @Index(name = "idx_wm_norm_sha256", columnList = "normalized_sha256"),
                @Index(name = "idx_wm_phash", columnList = "phash"),
                @Index(name = "idx_wm_user_created", columnList = "user_id, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wm_artifact", columnNames = {"artifact_id"})
        }
)
public class Watermark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long watermarkId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 아티팩트 고유 식별자 (UUID 문자열) */
    @Column(name = "artifact_id", nullable = false, length = 36, unique = true)
    private String artifactId;

    /** 삽입 메시지 (최대 4자) */
    @Column(nullable = false, length = 4)
    private String message;

    /** 원본 바이트 기준 정확매칭용 */
    @Column(nullable = false, length = 64)
    private String sha256;

    /** 정규화(무손실 PNG/EXIF적용) 후 SHA256 — 선택 */
    @Column(name = "normalized_sha256", length = 64)
    private String normalizedSha256;

    /** pHash 64bit (근사매칭용) */
    @Column(nullable = false)
    private Long phash;

    /** 결과물 S3 키(경로) */
    @Column(name = "s3_watermarked_key", nullable = false, length = 255)
    private String s3WatermarkedKey;

    /** message.txt S3 키(경로) */
    @Column(name = "s3_message_key", nullable = false, length = 255)
    private String s3MessageKey;

    @Column(nullable=false, length = 255)
    private String fileName;

    @Column(nullable=false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
