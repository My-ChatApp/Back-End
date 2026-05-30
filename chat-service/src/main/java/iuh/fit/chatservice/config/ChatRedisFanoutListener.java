package iuh.fit.chatservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.chatservice.event.payload.ChatRealtimeEnvelope;
import iuh.fit.chatservice.service.ChatRealtimeBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisFanoutListener {

    private static final String FANOUT_PREFIX = "chat:fanout:";

    private final ChatRealtimeBroadcastService broadcastService;
    private final ObjectMapper chatSpaceObjectMapper;

    /**
     * Spring MessageListenerAdapter: (Message, pattern bytes).
     * Không dùng (String, String) — tham số thứ hai là pattern {@code chat:fanout:*}, không phải channel thật.
     */
    public void handleRedisFanoutMessage(Message message, byte[] pattern) {
        if (message == null || message.getChannel() == null || message.getBody() == null) {
            return;
        }
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        if (!channel.startsWith(FANOUT_PREFIX)) {
            return;
        }
        String conversationId = channel.substring(FANOUT_PREFIX.length());
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            ChatRealtimeEnvelope envelope = chatSpaceObjectMapper.readValue(body, ChatRealtimeEnvelope.class);
            broadcastService.localBroadcast(conversationId, envelope);
        } catch (Exception e) {
            log.warn("Failed to handle fanout on {}: {}", channel, e.getMessage());
        }
    }
}
