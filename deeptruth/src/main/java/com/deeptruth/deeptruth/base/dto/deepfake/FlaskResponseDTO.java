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
    private DeepfakeResult result;

    @JsonProperty("average_fake_confidence")
    private Float averageConfidence;

    @JsonProperty("max_confidence")
    private Float maxConfidence;

    @JsonProperty("most_suspect_image")
    private String base64Url;

    private String detector;

    @JsonProperty("min_face")
    private Integer minFace;

    private String mode;

    @JsonProperty("sample_count")
    private Integer sampleCount;

    @JsonProperty("smooth_window")
    private Integer smoothWindow;

    @JsonProperty("use_illum")
    private Boolean useIllum;

    @JsonProperty("use_tta")
    private Boolean useTta;

    @JsonProperty("taskId")
    private String taskId;

    private String imageUrl;
}
