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
@Table(name = "watermarkDetection")
public class WatermarkDetection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long watermarkDetectionId;

    @OneToOne
    @JoinColumn(name = "watermark_id", unique = true, nullable = false)
    private Watermark watermark;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable=false, length = 255)
    private String originalFilePath;

    @Column(nullable=false, length = 255)
    private String resultFilePath;

    @Column
    private Float watermarkResult;

    @Column(nullable=false, updatable = false)
    private LocalDateTime watermarkDetectedAt;

    @PrePersist
    protected void onCreate() {
        this.watermarkDetectedAt = LocalDateTime.now();
    }
}
