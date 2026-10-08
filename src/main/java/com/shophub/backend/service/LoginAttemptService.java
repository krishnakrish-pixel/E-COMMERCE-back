package com.shophub.backend.service;

import com.shophub.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing: after 5 wrong passwords for an e-mail the account is locked for 5 minutes.
 * (Kept in memory: it resets when the server restarts, which is fine for a single-server shop.)
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_TIME = Duration.ofMinutes(5);
    private static final int MAX_TRACKED = 10_000;

    private static final class Attempt {
        int failures;
        Instant lockedUntil;
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** Call before checking the password. Throws 429 while the e-mail is locked. */
    public void assertAllowed(String email) {
        Attempt a = attempts.get(key(email));
        if (a != null && a.lockedUntil != null) {
            if (a.lockedUntil.isAfter(Instant.now())) {
                long mins = Math.max(1, Duration.between(Instant.now(), a.lockedUntil).toMinutes() + 1);
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many wrong passwords. Try again in " + mins + (mins == 1 ? " minute." : " minutes."));
            }
            attempts.remove(key(email));                 // lock has expired
        }
    }

    public void recordFailure(String email) {
        if (attempts.size() > MAX_TRACKED) {             // never let this map grow without limit
            Instant now = Instant.now();
            attempts.values().removeIf(x -> x.lockedUntil == null || x.lockedUntil.isBefore(now));
        }
        attempts.compute(key(email), (k, a) -> {
            Attempt next = a == null ? new Attempt() : a;
            next.failures++;
            if (next.failures >= MAX_FAILURES) next.lockedUntil = Instant.now().plus(LOCK_TIME);
            return next;
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
