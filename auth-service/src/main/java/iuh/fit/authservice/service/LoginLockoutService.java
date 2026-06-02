package iuh.fit.authservice.service;

import java.util.Optional;

public interface LoginLockoutService {

    record LockoutStatus(long retryAfterSeconds) {
    }

    Optional<LockoutStatus> getActiveLockout(String clientIp);

    /**
     * Records a failed login. Returns true when this attempt newly triggered an IP lockout.
     */
    boolean recordFailedAttempt(String clientIp, String email);

    void clearFailedAttempts(String clientIp);

    String formatLockoutMessage(long retryAfterSeconds);
}
