package com.deeptruth.deeptruth.base.dto.deepfake;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletDTO {
    private String key;
    private String label;
    private Float value;
    private Map<String, float[]> bands; // good/warn/bad
    private String direction;            // "lower" | "higher"
    private String unit;                 // "fps" | "ms" | null
}