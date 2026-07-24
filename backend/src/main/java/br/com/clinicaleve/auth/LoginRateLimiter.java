package br.com.clinicaleve.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private final int maxFailures;
    private final Duration window;
    private final Duration lockDuration;
    private final Clock clock;
    private final ConcurrentHashMap<LoginKey, AttemptState> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${security.login.max-failures}") int maxFailures,
            @Value("${security.login.window-minutes}") long windowMinutes,
            @Value("${security.login.lock-minutes}") long lockMinutes,
            Clock clock
    ) {
        this.maxFailures = Math.max(1, maxFailures);
        this.window = Duration.ofMinutes(Math.max(1, windowMinutes));
        this.lockDuration = Duration.ofMinutes(Math.max(1, lockMinutes));
        this.clock = clock;
    }

    public void assertAllowed(LoginKey key) {
        var now = clock.instant();
        var state = attempts.get(key);
        if (state == null) {
            return;
        }
        if (state.lockedUntil() != null && state.lockedUntil().isAfter(now)) {
            throw new LoginRateLimitException(Duration.between(now, state.lockedUntil()).toSeconds());
        }
        if (state.windowStarted().plus(window).isBefore(now)) {
            attempts.remove(key, state);
        }
    }

    public void recordFailure(LoginKey key) {
        var now = clock.instant();
        attempts.compute(key, (ignored, existing) -> {
            var current = existing;
            if (current == null || current.windowStarted().plus(window).isBefore(now)) {
                current = new AttemptState(now, 0, null);
            }
            var failures = current.failures() + 1;
            var lockedUntil = failures >= maxFailures ? now.plus(lockDuration) : null;
            return new AttemptState(current.windowStarted(), failures, lockedUntil);
        });
    }

    public void recordSuccess(LoginKey key) {
        attempts.remove(key);
    }

    public record LoginKey(String clinicSlug, String email, String remoteAddress) {
        public static LoginKey of(String clinicSlug, String email, String remoteAddress) {
            return new LoginKey(
                    normalize(clinicSlug),
                    normalize(email),
                    remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.trim()
            );
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }
    }

    private record AttemptState(Instant windowStarted, int failures, Instant lockedUntil) {
    }
}
