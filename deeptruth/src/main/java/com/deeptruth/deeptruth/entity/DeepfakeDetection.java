package com.deeptruth.deeptruth.entity;

import com.deeptruth.deeptruth.base.Enum.DeepfakeDetector;
import com.deeptruth.deeptruth.base.Enum.DeepfakeMode;
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

    @Enumerated(EnumType.STRING)
    @Column
    private DeepfakeMode mode;

    @Column
    private Boolean useTta;

    @Column
    private Boolean useIllum;

    @Enumerated(EnumType.STRING)
    @Column
    private DeepfakeDetector detector;

    @Column
    private Integer smoothWindow;

    @Column
    private Integer minFace;

    @Column
    private Integer sampleCount;

    @Column
    private String taskId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
