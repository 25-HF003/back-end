package com.deeptruth.deeptruth.base.dto.websocket;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgressDTO {
    private String taskId;
    private int progress;
}
