package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.websocket.ProgressDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/progress")
public class ProgressController {
    private final SimpMessagingTemplate messagingTemplate;

    // Flask에서 전송: POST /progress
    @PostMapping
    public ResponseEntity<Void> receiveProgress(@RequestBody ProgressDTO progressDto) {
        messagingTemplate.convertAndSend(
                "/topic/progress/" + progressDto.getTaskId(),
                progressDto
        );
        return ResponseEntity.ok().build();
    }
}
