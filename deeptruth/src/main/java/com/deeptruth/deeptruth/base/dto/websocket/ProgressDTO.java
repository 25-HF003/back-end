package com.deeptruth.deeptruth.base.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProgressDTO {
    private String taskId;
    private int progress;
    private String userId;
}
