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
        redisTemplate.opsForValue().set(PREFIX + request.getEmail(), request, TTL);
    }

    public RegisterRequest get(String email) {
        Object data =  redisTemplate.opsForValue().get(PREFIX + email);
        if (data == null) throw new IllegalStateException("Phiên đăng ký đã hết hạn, vui lòng thử lại");
        return objectMapper.convertValue(data, RegisterRequest.class);
    }

    public void delete(String email) {
        redisTemplate.delete(PREFIX + email);
    }
}