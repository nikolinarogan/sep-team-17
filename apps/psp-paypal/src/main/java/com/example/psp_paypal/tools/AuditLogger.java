package com.example.psp_paypal.tools;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
public class AuditLogger {

    private String lastHash = "PAYPAL_INIT_CHAIN_SECURE_2026";

    public synchronized void logEvent(String action, String status, String details) {
        String ipAddress = getClientIp();

        String user = "SYSTEM_OR_GUEST";

        String logMessage = String.format("User: %s | Action: %s | Status: %s | IP: %s | Details: %s",
                user, action, status, ipAddress, details);

        String currentHash = HashUtils.calculateSha256(logMessage + lastHash);

        log.info("[AUDIT_PAYPAL] {} | HASH: {} | CHAIN_PREV: {}", logMessage, currentHash, lastHash);

        lastHash = currentHash;
    }

    public synchronized void logSecurityAlert(String action, String reason) {
        String ipAddress = getClientIp();
        String user = "SYSTEM_OR_GUEST";

        String logMessage = String.format("SECURITY_ALERT | User: %s | Action: %s | Reason: %s | IP: %s",
                user, action, reason, ipAddress);

        String currentHash = HashUtils.calculateSha256(logMessage + lastHash);

        log.warn("[SECURITY_PAYPAL] {} | HASH: {} | CHAIN_PREV: {}", logMessage, currentHash, lastHash);
        lastHash = currentHash;
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
            return "SYSTEM"; // За background taskove
        }
        return "SYSTEM";
    }
}