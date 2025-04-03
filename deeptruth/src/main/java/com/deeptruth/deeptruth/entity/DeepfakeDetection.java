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
@Table(name = "deepfake_detection")
public class DeepfakeDetection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long DeepfakeDetectionId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable=false, length = 255)
    private String filePath;

    @Column
    private Float deepfakeResult;

    @Column
    private Float riskScore;

    @Column(columnDefinition = "JSON")
    private String detectedPart;

    @Column(nullable=false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
