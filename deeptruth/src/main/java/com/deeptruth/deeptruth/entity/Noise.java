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
@Table(name = "noise")
public class Noise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noiseId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable=false, length = 255)
    private String originalFilePath;

    @Column(nullable=false, length = 255)
    private String processedFilePath;

    @Column
    private Float epsilon = 0.03F;

    @Column
    private Boolean attackSuccess; // 공격 성공 여부

    @Column
    private String originalPrediction; // 기존 이미지 예측

    @Column
    private String adversarialPrediction; // 적대적 노이즈 적용 후 이미지 예측

    @Column(nullable=false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
