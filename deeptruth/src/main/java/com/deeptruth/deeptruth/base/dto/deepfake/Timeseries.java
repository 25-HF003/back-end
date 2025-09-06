package com.deeptruth.deeptruth.base.dto.deepfake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Timeseries {
    @JsonProperty("per_frame_conf")
    private List<Float> perFrameConf;
    private Float vmin; // 0.0
    private Float vmax; // 1.0
}
