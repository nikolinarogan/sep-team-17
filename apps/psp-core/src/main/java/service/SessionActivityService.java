package service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PCI DSS 8.2.8 – Idle timeout 15 minuta.
 * Prati poslednju aktivnost admina (in-memory) i proverava da li sesija još važi.
 */
@Service
public class SessionActivityService {
    private static final long IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 minuta

    private final Map<Long, Long> lastActivityByAdminId = new ConcurrentHashMap<>();

    public boolean isSessionValid(Long adminId) {
        Long lastActivity = lastActivityByAdminId.get(adminId);
        if (lastActivity == null) return true; // prvi zahtev nakon logina

        return System.currentTimeMillis() - lastActivity < IDLE_TIMEOUT_MS;
    }

    public void updateActivity(Long adminId) {
        lastActivityByAdminId.put(adminId, System.currentTimeMillis());
    }

    public void clearActivity(Long adminId) {
        lastActivityByAdminId.remove(adminId);
    }
}
