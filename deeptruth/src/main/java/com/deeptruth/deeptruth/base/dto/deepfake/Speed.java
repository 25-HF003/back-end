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
public class Speed {
    @JsonProperty("fps_processed")
    private Float fpsProcessed;

    @JsonProperty("ms_per_sample")
    private Float msPerSample;

    @JsonProperty("target_fps")
    private Float targetFps;

    @JsonProperty("max_latency_ms")
    private Float maxLatencyMs;

    @JsonProperty("speed_ok")
    private Boolean speedOk;

}
