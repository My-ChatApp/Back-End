package iuh.fit.chatservice.presence;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PresenceSessionTracker {

    private final ConcurrentHashMap<String, AtomicInteger> activeSessions = new ConcurrentHashMap<>();

    /**
     * @return true nếu đây là session đầu tiên của user (cần publish online)
     */
    public boolean markConnected(String userId) {
        return activeSessions
                .computeIfAbsent(userId, ignored -> new AtomicInteger())
                .incrementAndGet() == 1;
    }

    /**
     * @return true nếu user không còn session nào (cần publish offline)
     */
    public boolean markDisconnected(String userId) {
        AtomicInteger count = activeSessions.get(userId);
        if (count == null) {
            return false;
        }
        int remaining = count.decrementAndGet();
        if (remaining <= 0) {
            activeSessions.remove(userId);
            return true;
        }
        return false;
    }
}
