package iuh.fit.userservice.security;

import iuh.fit.common.security.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-UserId");
        String email = request.getHeader("X-Email");

        log.info("[UserContextFilter] X-UserId: {}, X-Email: {}", userId, email);

        if (userId != null) {
            log.info("[UserContextFilter] Setting authentication for userId: {}", userId);

            CurrentUser currentUser =
                    new CurrentUser(userId, email);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            currentUser,
                            null,
                            List.of()
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(auth);
            log.info("[UserContextFilter] Authentication set successfully");
        } else {
            log.warn("[UserContextFilter] X-UserId header is null - authentication NOT set");
        }

        filterChain.doFilter(request, response);
    }
}
