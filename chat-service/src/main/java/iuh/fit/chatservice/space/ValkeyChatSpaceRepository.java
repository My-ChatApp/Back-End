package iuh.fit.chatservice.space;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.chatservice.config.ChatSpaceProperties;
import iuh.fit.chatservice.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.space.enabled", havingValue = "true", matchIfMissing = true)
public class ValkeyChatSpaceRepository implements ChatSpaceRepository {

    private static final String TIMELINE_KEY = "chat:conv:%s:timeline";
    private static final String MSG_KEY = "chat:msg:%s";
    private static final String HYDRATE_LOCK_KEY = "chat:conv:%s:hydrate:lock";

    private final StringRedisTemplate redis;
    private final ObjectMapper chatSpaceObjectMapper;
    private final ChatSpaceProperties properties;

    @Override
    public void appendMessage(ChatMessage message) {
        appendMessagesBatch(List.of(message));
    }

    @Override
    public void appendMessagesBatch(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (ChatMessage message : messages) {
            String timelineKey = timelineKey(message.getConversationId());
            String msgKey = msgKey(message.getMessageId());
            double score = message.getCreatedAt() != null
                    ? message.getCreatedAt().toEpochMilli()
                    : Instant.now().toEpochMilli();
            try {
                redis.opsForValue().set(msgKey, chatSpaceObjectMapper.writeValueAsString(message));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize ChatMessage " + message.getMessageId(), e);
            }
            redis.opsForZSet().add(timelineKey, message.getMessageId(), score);
        }
        String conversationId = messages.get(0).getConversationId();
        trimTimeline(conversationId);
    }

    @Override
    public List<ChatMessage> getRecent(String conversationId, int limit) {
        Set<String> ids = redis.opsForZSet().reverseRange(timelineKey(conversationId), 0, limit - 1L);
        return loadMessages(ids);
    }

    @Override
    public List<ChatMessage> getBefore(String conversationId, String beforeMessageId, int limit) {
        Optional<ChatMessage> before = getMessage(beforeMessageId);
        if (before.isEmpty() || before.get().getCreatedAt() == null) {
            return List.of();
        }
        double maxScore = before.get().getCreatedAt().toEpochMilli() - 1;
        Set<String> ids = redis.opsForZSet().reverseRangeByScore(
                timelineKey(conversationId),
                Double.NEGATIVE_INFINITY,
                maxScore,
                0,
                limit);
        return loadMessages(ids);
    }

    @Override
    public Optional<ChatMessage> getMessage(String messageId) {
        String json = redis.opsForValue().get(msgKey(messageId));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(chatSpaceObjectMapper.readValue(json, ChatMessage.class));
        } catch (JsonProcessingException e) {
            log.warn("Invalid message JSON for id {}: {}", messageId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void updateMessage(ChatMessage message) {
        try {
            redis.opsForValue().set(msgKey(message.getMessageId()), chatSpaceObjectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ChatMessage " + message.getMessageId(), e);
        }
    }

    @Override
    public long countInTimeline(String conversationId) {
        Long size = redis.opsForZSet().size(timelineKey(conversationId));
        return size != null ? size : 0;
    }

    @Override
    public boolean tryAcquireHydrateLock(String conversationId, int ttlSeconds) {
        Boolean ok = redis.opsForValue().setIfAbsent(
                hydrateLockKey(conversationId),
                "1",
                Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public void releaseHydrateLock(String conversationId) {
        redis.delete(hydrateLockKey(conversationId));
    }

    private void trimTimeline(String conversationId) {
        int max = properties.getTimelineMaxSize();
        Long size = redis.opsForZSet().size(timelineKey(conversationId));
        if (size != null && size > max) {
            redis.opsForZSet().removeRange(timelineKey(conversationId), 0, size - max - 1);
        }
    }

    private List<ChatMessage> loadMessages(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ChatMessage> result = new ArrayList<>();
        for (String id : ids) {
            getMessage(id).ifPresent(result::add);
        }
        Collections.reverse(result);
        return result;
    }

    private static String timelineKey(String conversationId) {
        return TIMELINE_KEY.formatted(conversationId);
    }

    private static String msgKey(String messageId) {
        return MSG_KEY.formatted(messageId);
    }

    private static String hydrateLockKey(String conversationId) {
        return HYDRATE_LOCK_KEY.formatted(conversationId);
    }
}
