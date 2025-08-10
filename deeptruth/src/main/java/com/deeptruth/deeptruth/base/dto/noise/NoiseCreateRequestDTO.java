package com.deeptruth.deeptruth.base.dto.noise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoiseCreateRequestDTO {

    private String originalFilePath;
    private Float epsilon;

    public void validate() {
        if (originalFilePath == null || originalFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("원본 파일 경로는 필수입니다");
        }
        if (epsilon == null || epsilon <= 0) {
            throw new IllegalArgumentException("엡실론 값은 0보다 큰 값이어야 합니다");
        }
    }
}
