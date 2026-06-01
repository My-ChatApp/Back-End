package iuh.fit.authservice.service;

import iuh.fit.authservice.dto.request.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PendingRegistrationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "pending:reg:";
    private static final Duration TTL = Duration.ofMinutes(10);

    @Autowired
    private ObjectMapper objectMapper;

    public void save(RegisterRequest request) {
        String key = PREFIX + normalize(request.getEmail());
        redisTemplate.opsForValue().set(key, request, TTL);
    }

    public boolean hasPending(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + normalize(email)));
    }

    public RegisterRequest get(String email) {
        Object data = redisTemplate.opsForValue().get(PREFIX + normalize(email));
        if (data == null) throw new IllegalStateException("Phiên đăng ký đã hết hạn, vui lòng thử lại");
        return objectMapper.convertValue(data, RegisterRequest.class);
    }

    public void delete(String email) {
        redisTemplate.delete(PREFIX + normalize(email));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}