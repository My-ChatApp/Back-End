package iuh.fit.chatservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.chatservice.event.payload.ChatRealtimeEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRealtimeBroadcastService {

    private static final String FANOUT_CHANNEL = "chat:fanout:%s";

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redis;
    private final ObjectMapper chatSpaceObjectMapper;

    @Value("${mychatapp.websocket.broker.topic-prefix:/topic}")
    private String topicPrefix;

    @Value("${chat.space.redis-fanout-enabled:false}")
    private boolean redisFanoutEnabled;

    public void broadcast(String conversationId, ChatRealtimeEnvelope envelope) {
        if (redisFanoutEnabled) {
            try {
                String payload = chatSpaceObjectMapper.writeValueAsString(envelope);
                redis.convertAndSend(FANOUT_CHANNEL.formatted(conversationId), payload);
            } catch (JsonProcessingException e) {
                log.warn("Redis fanout serialize failed, falling back to local WS: {}", e.getMessage());
                localBroadcast(conversationId, envelope);
            }
        } else {
            localBroadcast(conversationId, envelope);
        }
    }

    public void localBroadcast(String conversationId, ChatRealtimeEnvelope envelope) {
        Object payload = envelope.getEventType() == ChatRealtimeEnvelope.EventType.MESSAGE_CREATED
                ? envelope.getMessage()
                : envelope;
        messagingTemplate.convertAndSend(topicPrefix + "/conversation/" + conversationId, payload);
    }
}
