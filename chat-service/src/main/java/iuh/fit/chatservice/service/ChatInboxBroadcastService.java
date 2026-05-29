package iuh.fit.chatservice.service;

import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.event.payload.ChatInboxEvent;
import iuh.fit.chatservice.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatInboxBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${mychatapp.websocket.broker.topic-prefix:/topic}")
    private String topicPrefix;

    public void notifyConversationCreated(String targetUserId, Conversation conversation) {
        if (targetUserId == null || conversation == null) {
            return;
        }
        messagingTemplate.convertAndSend(
                inboxDestination(targetUserId),
                ChatInboxEvent.builder()
                        .eventType(ChatInboxEvent.EventType.CONVERSATION_CREATED)
                        .conversation(conversation)
                        .build());
    }

    public void notifyMessageCreated(String targetUserId, Conversation conversation, ChatMessage message) {
        if (targetUserId == null || message == null) {
            return;
        }
        messagingTemplate.convertAndSend(
                inboxDestination(targetUserId),
                ChatInboxEvent.builder()
                        .eventType(ChatInboxEvent.EventType.MESSAGE_CREATED)
                        .conversation(conversation)
                        .message(message)
                        .build());
    }

    public void notifyConversationUpdated(String targetUserId, Conversation conversation) {
        if (targetUserId == null || conversation == null) {
            return;
        }
        messagingTemplate.convertAndSend(
                inboxDestination(targetUserId),
                ChatInboxEvent.builder()
                        .eventType(ChatInboxEvent.EventType.CONVERSATION_UPDATED)
                        .conversation(conversation)
                        .build());
    }

    public void notifyConversationDeleted(String targetUserId, Conversation conversation) {
        if (targetUserId == null || conversation == null) {
            return;
        }
        messagingTemplate.convertAndSend(
                inboxDestination(targetUserId),
                ChatInboxEvent.builder()
                        .eventType(ChatInboxEvent.EventType.CONVERSATION_DELETED)
                        .conversation(conversation)
                        .build());
    }

    private String inboxDestination(String userId) {
        return topicPrefix + "/inbox/" + userId;
    }
}
