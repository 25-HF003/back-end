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

    @Column(nullable=false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
