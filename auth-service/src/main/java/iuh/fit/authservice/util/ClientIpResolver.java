package iuh.fit.authservice.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private static final String UNKNOWN = "unknown";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        if (request.getRemoteAddr() != null && !request.getRemoteAddr().isBlank()) {
            return request.getRemoteAddr();
        }
        return UNKNOWN;
    }
}
