package com.deeptruth.deeptruth.base.dto.noise;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoiseFlaskResponseDTO {

    @JsonProperty("originalFilePath")
    private String originalFilePath;

    @JsonProperty("processedFilePath")
    private String processedFilePath;

    @JsonProperty("epsilon")
    private Float epsilon;

    @JsonProperty("attackSuccess")
    private Boolean attackSuccess;

    @JsonProperty("originalPrediction")
    private String originalPrediction;

    @JsonProperty("adversarialPrediction")
    private String adversarialPrediction;

    @JsonProperty("originalConfidence")
    private String originalConfidence;

    @JsonProperty("adversarialConfidence")
    private String adversarialConfidence;

    @JsonProperty("confidenceDrop")
    private String confidenceDrop;

    private String message;
}
