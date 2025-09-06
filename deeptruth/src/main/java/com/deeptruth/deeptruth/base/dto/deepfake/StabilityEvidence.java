package com.deeptruth.deeptruth.base.dto.deepfake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StabilityEvidence {
    @JsonProperty("temporal_delta_mean")
    private Float temporalDeltaMean;

    @JsonProperty("temporal_delta_std")
    private Float temporalDeltaStd;

    @JsonProperty("tta_mean")
    private Float ttaMean;

    @JsonProperty("tta_std")
    private Float ttaStd;
}
