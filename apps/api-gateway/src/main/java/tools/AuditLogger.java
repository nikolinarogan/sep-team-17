package tools;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditLogger {

    public void logEvent(HttpServletRequest request, String action, String status, String details) {
        String ipAddress = getIpFromRequest(request);
        log.info("[AUDIT] Action: {} | Status: {} | IP: {} | Details: {}",
                action, status, ipAddress, details);
    }

    public void logSecurityAlert(HttpServletRequest request, String action, String reason) {
        String ipAddress = getIpFromRequest(request);
        log.warn("[SECURITY_ALERT] Action: {} | Reason: {} | IP: {}",
                action, reason, ipAddress);
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