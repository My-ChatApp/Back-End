package iuh.fit.chatservice.presence;

import iuh.fit.chatservice.event.publisher.PresencePublisher;
import iuh.fit.chatservice.security.StompAuthChannelInterceptor;
import iuh.fit.chatservice.security.StompPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Presence is tied to STOMP CONNECT (JWT validated, userId in session), not raw WebSocket open.
 * {@link org.springframework.web.socket.messaging.SessionConnectedEvent} fires too early — before CONNECT.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketPresenceListener {

    private final PresenceSessionTracker sessionTracker;
    private final PresencePublisher presencePublisher;

    @EventListener
    public void onStompConnect(SessionConnectEvent event) {
        String userId = resolveUserId(event.getMessage());
        if (userId == null) {
            log.warn("STOMP CONNECT without userId — skipping presence online");
            return;
        }
        if (sessionTracker.markConnected(userId)) {
            log.info("User {} online (first STOMP session)", userId);
            presencePublisher.publishPresenceChanged(userId, true);
        }
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        String userId = resolveUserId(event.getMessage());
        if (userId == null) {
            return;
        }
        if (sessionTracker.markDisconnected(userId)) {
            log.info("User {} offline (last STOMP session closed)", userId);
            presencePublisher.publishPresenceChanged(userId, false);
        }
    }

    private static String resolveUserId(org.springframework.messaging.Message<?> message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getUser() instanceof StompPrincipal principal) {
            String fromPrincipal = principal.userId();
            if (fromPrincipal != null && !fromPrincipal.isBlank()) {
                return fromPrincipal;
            }
        }
        if (accessor.getSessionAttributes() != null) {
            Object userId = accessor.getSessionAttributes().get(StompAuthChannelInterceptor.SESSION_USER_ID);
            if (userId != null) {
                return userId.toString();
            }
        }
        return null;
    }
}
