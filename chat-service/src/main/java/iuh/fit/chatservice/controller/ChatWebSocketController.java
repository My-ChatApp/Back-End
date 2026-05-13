package iuh.fit.chatservice.controller;

import iuh.fit.chatservice.dto.request.SendMessageRequest;
import iuh.fit.chatservice.entity.Message;
import iuh.fit.chatservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(SendMessageRequest request) {
        Message saved = chatService.sendMessage(request);

        // Chỉ push tới đúng conversation
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + request.getConversationId(),
                saved
        );
    }
}