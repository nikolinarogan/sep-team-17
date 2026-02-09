package com.ws.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionActivityService {
    private static final long IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 minuta

    private final Map<Long, Long> lastActivityByUserId = new ConcurrentHashMap<>();

    public boolean isSessionValid(Long userId) {
        Long lastActivity = lastActivityByUserId.get(userId);
        if (lastActivity == null) return true; // prvi zahtev nakon logina

        return System.currentTimeMillis() - lastActivity < IDLE_TIMEOUT_MS;
    }

    public void updateActivity(Long userId) {
        lastActivityByUserId.put(userId, System.currentTimeMillis());
    }

    public void clearActivity(Long userId) {
        lastActivityByUserId.remove(userId);
    }
}
