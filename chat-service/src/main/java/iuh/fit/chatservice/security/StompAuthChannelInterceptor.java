package iuh.fit.chatservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Validates JWT on STOMP CONNECT (where {@code Authorization: Bearer} is available).
 * HTTP SockJS handshake (/ws/info) cannot carry that header — see gateway {@code AuthFilter}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_EMAIL = "email";

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
            return message;
        }

        if (requiresAuthenticatedSession(command) && accessor.getUser() == null) {
            log.warn("[StompAuth] Rejected {} — no authenticated STOMP session", command);
            throw new IllegalArgumentException("Unauthorized STOMP session");
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = resolveBearerToken(accessor);
        if (token == null || !jwtService.isValid(token)) {
            log.warn("[StompAuth] CONNECT rejected — missing or invalid JWT");
            throw new IllegalArgumentException("Unauthorized STOMP CONNECT");
        }

        String userId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);
        if (userId == null || userId.isBlank()) {
            log.warn("[StompAuth] CONNECT rejected — token has no userId claim");
            throw new IllegalArgumentException("Unauthorized STOMP CONNECT");
        }

        StompPrincipal principal = new StompPrincipal(userId, email);
        accessor.setUser(principal);
        accessor.getSessionAttributes().put(SESSION_USER_ID, userId);
        if (email != null) {
            accessor.getSessionAttributes().put(SESSION_EMAIL, email);
        }

        log.info("[StompAuth] CONNECT accepted userId={}", userId);
    }

    private static boolean requiresAuthenticatedSession(StompCommand command) {
        return StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command);
    }

    private static String resolveBearerToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        String accessToken = accessor.getFirstNativeHeader("access_token");
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken.trim();
        }
        return null;
    }
}
