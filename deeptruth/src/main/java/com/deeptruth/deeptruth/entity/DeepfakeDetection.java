package com.deeptruth.deeptruth.entity;

import com.deeptruth.deeptruth.base.Enum.DeepfakeDetector;
import com.deeptruth.deeptruth.base.Enum.DeepfakeMode;
import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import com.deeptruth.deeptruth.base.dto.deepfake.Timeseries;
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

    @Column(length = 255) private String filePath;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DeepfakeResult result;
    @Column(length = 64) private String taskId;

    @Column private Float scoreWeighted;
    @Column(name = "threshold_tau") private Float thresholdTau;
    @Column private Float frameVoteRatio;

    @Column private Float averageConfidence;
    @Column private Float medianConfidence;
    @Column private Float maxConfidence;
    @Column private Float varianceConfidence;

    @Column private Integer framesProcessed;
    @Column private Float processingTimeSec;

    @Column private Float fpsProcessed;
    @Column private Float msPerSample;
    @Column private Float targetFpsUsed;
    @Column private Float maxLatencyMsUsed;
    @Column private Boolean speedOk;

    @Column private Float temporalDeltaMean;
    @Column private Float temporalDeltaStd;
    @Column private Float ttaMean;
    @Column private Float ttaStd;

    @Lob @Column(columnDefinition = "TEXT") private String timeseriesJson;

    @Column private Float speedScore;
    @Column private Float stabilityScore;

    @Enumerated(EnumType.STRING) @Column private DeepfakeMode mode;
    @Enumerated(EnumType.STRING) @Column private DeepfakeDetector detector;
    @Column private Boolean useTta;
    @Column private Boolean useIllum;
    @Column private Integer smoothWindow;
    @Column private Integer minFace;
    @Column private Integer sampleCount;

    @Column(nullable=false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
