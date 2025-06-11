package com.deeptruth.deeptruth.base.dto.deepfake;

import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlaskResponseDTO {
    private DeepfakeResult result;

    @JsonProperty("average_fake_confidence")
    private Float averageConfidence;

    @JsonProperty("max_confidence")
    private Float maxConfidence;

    @JsonProperty("most_suspect_image")
    private String base64Url;

    private String imageUrl;
}
