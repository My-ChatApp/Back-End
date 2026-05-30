package iuh.fit.chatservice.controller;

import iuh.fit.chatservice.dto.request.SendMessageRequest;
import iuh.fit.chatservice.service.ChatCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatCommandService chatCommandService;

    @MessageMapping("/chat.send")
    public void sendMessage(SendMessageRequest request) {
        chatCommandService.sendMessage(request);
    }
}
