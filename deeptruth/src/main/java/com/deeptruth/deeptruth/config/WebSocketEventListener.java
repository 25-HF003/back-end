package com.deeptruth.deeptruth.config;

import com.deeptruth.deeptruth.service.ActiveTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final SimpMessageSendingOperations messagingTemplate;
    private final ActiveTaskService activeTaskService;

    // 클라이언트 연결 시 실행
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("[Connected] 세션 ID: {}", sessionId);

        var acc = StompHeaderAccessor.wrap(event.getMessage());
        var p = acc.getUser();
        log.info("[WS] principalClass={}, name={}",
                p != null ? p.getClass().getSimpleName() : "null",
                p != null ? p.getName() : "null");
    }

    // 클라이언트 연결 해제 시 실행
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("[Disconnected] 세션 ID: {}", sessionId);

        Principal principal = headerAccessor.getUser();

        // Principal을 CustomUserDetails 타입으로 변환
        if (principal instanceof CustomUserDetails userDetails) {

            Long userId = userDetails.getUserId();
            String loginId = userDetails.getUsername();

            String taskId = activeTaskService.getActiveTask(loginId);

            if (taskId != null) {
                log.warn("사용자 '{}'(ID:{})의 연결이 끊겼습니다. 진행 중인 작업 '{}'을(를) 목록에서 제거합니다.",
                        loginId, userId, taskId);
                activeTaskService.deregisterTask(loginId);
            }
        }
    }
}