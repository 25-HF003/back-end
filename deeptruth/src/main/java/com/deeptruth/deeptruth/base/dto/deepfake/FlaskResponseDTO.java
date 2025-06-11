package com.deeptruth.deeptruth.base.dto.deepfake;

import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlaskResponseDTO {
    private DeepfakeResult result;
    private Float confidence;
    private String base64Url;
    private String imageUrl;
}
