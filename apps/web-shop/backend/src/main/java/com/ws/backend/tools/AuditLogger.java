package com.ws.backend.tools;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    public void logEvent(String action, String status, String details) {
        String ipAddress = getClientIp();
        log.info("[AUDIT] Action: {} | Status: {} | IP: {} | Details: {}",
                action, status, ipAddress, details);
    }

    public void logSecurityAlert(String action, String reason) {
        String ipAddress = getClientIp();
        log.warn("[SECURITY_ALERT] Action: {} | Reason: {} | IP: {}",
                action, reason, ipAddress);
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0];
            }
            return request.getRemoteAddr();
        }
        return "SYSTEM"; // za @Scheduled taskove koji nemaju HTTP zahtev
    }
}
