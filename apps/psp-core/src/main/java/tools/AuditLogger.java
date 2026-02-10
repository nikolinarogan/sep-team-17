package tools;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
public class AuditLogger {

    private String lastHash = "INIT_HASH_SEED_2026";

    public synchronized void logEvent(String action, String status, String details) {
        String ipAddress = getClientIp();
        String user = getCurrentUser();

        String logMessage = String.format("User: %s | Action: %s | Status: %s | IP: %s | Details: %s",
                user, action, status, ipAddress, details);

        // Sadržaj ove poruke + Heš prošle poruke
        String currentHash = HashUtils.calculateSha256(logMessage + lastHash);

        log.info("[AUDIT] {} | HASH: {} | CHAIN_PREV: {}", logMessage, currentHash, lastHash);

        lastHash = currentHash;
    }

    public synchronized void logSecurityAlert(String action, String reason) {
        String ipAddress = getClientIp();
        String user = getCurrentUser();

        String logMessage = String.format("SECURITY_ALERT | User: %s | Action: %s | Reason: %s | IP: %s",
                user, action, reason, ipAddress);

        String currentHash = HashUtils.calculateSha256(logMessage + lastHash);

        log.warn("[SECURITY] {} | HASH: {} | CHAIN_PREV: {}", logMessage, currentHash, lastHash);
        lastHash = currentHash;
    }

    private String getCurrentUser() {
        try {
            // Za Admine i Merchante
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                return "AUTH:" + auth.getName();
            }

            // Ako nije ulogovan, proveri sesiju (za kupce)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpSession session = attributes.getRequest().getSession(false);
                if (session != null) {
                    return "GUEST_SESSION:" + session.getId().substring(0, 8);
                }
            }
        } catch (Exception e) {
            return "SYSTEM_INTERNAL";
        }
        return "GUEST_UNKNOWN";
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0];
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            return "SYSTEM"; // Za Scheduled taskove
        }
        return "SYSTEM";
    }
}