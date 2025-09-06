package com.deeptruth.deeptruth.base.dto.deepfake;

import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlaskResponseDTO {
    @JsonProperty("taskId") private String taskId;
    private DeepfakeResult result;
    private String imageUrl;
    @JsonProperty("most_suspect_image") private String base64Url;


    @JsonProperty("score_weighted") private Float scoreWeighted;
    @JsonProperty("threshold_tau") private Float thresholdTau;
    @JsonProperty("frame_vote_ratio") private Float frameVoteRatio;

    @JsonProperty("average_fake_confidence") private Float averageConfidence;
    @JsonProperty("median_confidence") private Float medianConfidence;
    @JsonProperty("max_confidence") private Float maxConfidence;
    @JsonProperty("variance_confidence") private Float varianceConfidence;

    @JsonProperty("frames_processed") private Integer framesProcessed;
    @JsonProperty("processing_time_sec") private Float processingTimeSec;

    private String detector;
    private String mode;
    @JsonProperty("min_face") private Integer minFace;
    @JsonProperty("sample_count") private Integer sampleCount;
    @JsonProperty("smooth_window") private Integer smoothWindow;
    @JsonProperty("use_illum") private Boolean useIllum;
    @JsonProperty("use_tta") private Boolean useTta;

    @JsonProperty("timeseries") private Timeseries timeseries;
    @JsonProperty("stability_evidence") private StabilityEvidence stabilityEvidence;
    @JsonProperty("speed") private Speed speed;

}
