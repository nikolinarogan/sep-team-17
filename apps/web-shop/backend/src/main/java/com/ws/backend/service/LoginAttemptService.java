package com.ws.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blokira prijavu nakon 10 neuspešnih pokušaja na 30 minuta.
 */
@Service
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 10;
    private static final int LOCKOUT_MINUTES = 30;

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public void recordFailedAttempt(String identifier) {
        attempts.compute(identifier, (k, v) -> {
            if (v == null) v = new AttemptInfo();
            v.count++;
            if (v.count >= MAX_ATTEMPTS) {
                v.lockoutUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
            }
            return v;
        });
    }

    public boolean isLockedOut(String identifier) {
        AttemptInfo info = attempts.get(identifier);
        if (info == null || info.lockoutUntil == null) return false;
        if (LocalDateTime.now().isBefore(info.lockoutUntil)) {
            return true;
        }
        attempts.remove(identifier);
        return false;
    }

    /** Vraća preostale minute lockout-a, ili 0 ako nije zaključan. */
    public long getRemainingLockoutMinutes(String identifier) {
        AttemptInfo info = attempts.get(identifier);
        if (info == null || info.lockoutUntil == null) return 0;
        long remaining = java.time.Duration.between(LocalDateTime.now(), info.lockoutUntil).toMinutes();
        return Math.max(0, remaining);
    }

    public void clearAttempts(String identifier) {
        attempts.remove(identifier);
    }

    private static class AttemptInfo {
        int count;
        LocalDateTime lockoutUntil;
    }
}
