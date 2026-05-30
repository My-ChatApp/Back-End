package iuh.fit.chatservice.security;

import java.security.Principal;

/**
 * STOMP session principal — {@link #getName()} returns userId (UUID string).
 */
public record StompPrincipal(String userId, String email) implements Principal {

    @Override
    public String getName() {
        return userId;
    }
}
