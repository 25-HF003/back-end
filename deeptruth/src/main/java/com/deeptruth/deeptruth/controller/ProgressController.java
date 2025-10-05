package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.websocket.ProgressDTO;
import com.deeptruth.deeptruth.config.CustomUserDetails;
import com.deeptruth.deeptruth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.attribute.UserPrincipal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/progress")
public class ProgressController {
    private final SimpMessagingTemplate messagingTemplate;

    // Flask에서 전송: POST /progress
    @PostMapping
    public ResponseEntity<Void> receiveProgress(@RequestBody ProgressDTO progressDto) {

        String loginId = progressDto.getLoginId();

        messagingTemplate.convertAndSendToUser(
                loginId,
                "/queue/progress/" + progressDto.getTaskId(),
                progressDto
        );
        log.info("progress:{}",progressDto.getProgress());
        return ResponseEntity.ok().build();
    }
}
