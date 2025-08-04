package com.deeptruth.deeptruth.base.dto.watermark;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatermarkFlaskResponseDTO {
    private String image_base64;
    private String filename;
    private String message;
    private String watermarkedFilePath;
}
