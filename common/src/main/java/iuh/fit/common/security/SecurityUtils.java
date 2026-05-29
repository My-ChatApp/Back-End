package iuh.fit.common.security;

import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static CurrentUser getCurrentUser() {
        return (CurrentUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    public static String getUserId() {
        return getCurrentUser().getUserId();
    }
}