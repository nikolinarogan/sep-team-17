package tools;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditLogger {

    private String lastHash = "GATEWAY_INIT_CHAIN_2026";

    public synchronized void logEvent(HttpServletRequest request, String action, String status, String details) {
        String ipAddress = getIpFromRequest(request);
        String user = identifyUser(request);

        String logContent = String.format("User: %s | Action: %s | Status: %s | IP: %s | Details: %s",
                user, action, status, ipAddress, details);

        String currentHash = HashUtils.calculateSha256(logContent + lastHash);

        log.info("[AUDIT_GATEWAY] {} | HASH: {} | CHAIN_PREV: {}", logContent, currentHash, lastHash);

        lastHash = currentHash;
    }

    public synchronized void logSecurityAlert(HttpServletRequest request, String action, String reason) {
        String ipAddress = getIpFromRequest(request);
        String user = identifyUser(request);

        String logContent = String.format("SECURITY_ALERT | User: %s | Action: %s | Reason: %s | IP: %s",
                user, action, reason, ipAddress);

        String currentHash = HashUtils.calculateSha256(logContent + lastHash);

        log.warn("[SECURITY_GATEWAY] {} | HASH: {} | CHAIN_PREV: {}", logContent, currentHash, lastHash);
        lastHash = currentHash;
    }

    private String identifyUser(HttpServletRequest request) {
        if (request == null) return "SYSTEM";

        // 1. Ako je Gateway već uradio autentifikaciju (npr. prepoznao admina)
        if (request.getUserPrincipal() != null) {
            return "AUTH:" + request.getUserPrincipal().getName();
        }

        // 2. Ako postoji Bearer token u headeru (ne znamo ko je, ali znamo da ima token)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return "BEARER_TOKEN_USER";
        }

        // 3. Provera sesije (ako postoji JSESSIONID)
        HttpSession session = request.getSession(false);
        if (session != null) {
            return "SESSION:" + session.getId().substring(0, 8);
        }

        // 4. Ako nema ništa, onda je Gost identifikovan IP adresom
        return "GUEST_IP:" + getIpFromRequest(request);
    }

    private String getIpFromRequest(HttpServletRequest request) {
        if (request == null) return "SYSTEM";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}