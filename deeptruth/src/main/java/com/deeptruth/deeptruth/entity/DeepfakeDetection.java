package com.deeptruth.deeptruth.entity;

import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
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
    private Long deepfakeDetectionId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeepfakeResult result;

    @Column
    private Float averageConfidence;
    @Column
    private Float maxConfidence;

    @Column(nullable=false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
