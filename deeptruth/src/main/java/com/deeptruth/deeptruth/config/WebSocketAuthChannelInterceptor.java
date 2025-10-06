package com.deeptruth.deeptruth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null) return message;

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            // 프론트에서 connectHeaders: { loginId: "..." } 로 보낸 값 읽기
            String loginId = acc.getFirstNativeHeader("loginId");
            if (loginId != null && !loginId.isBlank()) {
                acc.setUser(new UsernamePasswordAuthenticationToken(loginId, null, List.of()));
                log.info("[WS-AUTH] CONNECT as {}", loginId);
            } else {
                log.warn("[WS-AUTH] CONNECT without loginId header");
            }
        }

        if (StompCommand.SUBSCRIBE.equals(acc.getCommand())) {
            log.info("[WS] SUBSCRIBE dest={}, principal={}",
                    acc.getDestination(),
                    acc.getUser() != null ? acc.getUser().getName() : "null");
        }

        return message;
    }
}

