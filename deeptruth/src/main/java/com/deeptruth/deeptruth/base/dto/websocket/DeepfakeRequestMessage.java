package com.deeptruth.deeptruth.base.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeepfakeRequestMessage {
    private String taskId;
    private Long userId;
    private String loginId;

    // 경로 A
    private byte[] fileBytes;
    private String originalFilename;
    private String contentType;
    private long fileSize;

    // 경로 B
    private String s3Key;

    // 옵션
    private String mode;
    private String detector;
    private String useTta;
    private String useIllum;
    private String minFace;
    private String sampleCount;
    private String smoothWindow;
}
