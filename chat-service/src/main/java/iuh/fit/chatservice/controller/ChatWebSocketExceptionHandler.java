package iuh.fit.chatservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@Slf4j
public class ChatWebSocketExceptionHandler {

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> handleException(Throwable ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Gửi tin nhắn thất bại";
        log.warn("[WebSocket] Message handler error: {}", message);
        return Map.of("message", message);
    }
}
