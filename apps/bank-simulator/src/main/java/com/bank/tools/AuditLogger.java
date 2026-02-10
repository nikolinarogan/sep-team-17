package com.bank.tools;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class AuditLogger {

    private String lastHash = "BANK_GENESIS_BLOCK_SECURE_2026";

    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d{4}[ -]?){3,4}\\d{1,4}\\b");
    private static final Pattern SENSITIVE_DATA_PATTERN = Pattern.compile("(?i)(cvv|cvc|pin|password|merchantPassword)[\"']?\\s*[:=]\\s*[\"']?(\\w+?)[\"']?(?=\\s|,|}|\\b)");

    public synchronized void logEvent(String action, String status, String details) {
        String ipAddress = getClientIp();
        String user = "SYSTEM";

        String safeDetails = maskSensitiveData(details);

        String logMessage = String.format("User: %s | Action: %s | Status: %s | IP: %s | Details: %s",
                user, action, status, ipAddress, safeDetails);

        String currentHash = HashUtils.calculateSha256(logMessage + lastHash);

        log.info("[AUDIT_BANK] {} | HASH: {} | CHAIN_PREV: {}", logMessage, currentHash, lastHash);

        lastHash = currentHash;
    }

    public synchronized void logSecurityAlert(String action, String reason) {
        String ipAddress = getClientIp();

        String safeReason = maskSensitiveData(reason);

        String logMessage = String.format("SECURITY_ALERT | Action: %s | Reason: %s | IP: %s",
                action, safeReason, ipAddress);

        String currentHash = HashUtils.calculateSha256(logMessage + lastHash);

        log.warn("[SECURITY_BANK] {} | HASH: {} | CHAIN_PREV: {}", logMessage, currentHash, lastHash);
        lastHash = currentHash;
    }

    private String maskSensitiveData(String input) {
        if (input == null || input.isEmpty()) return input;
        String output = input;

        Matcher sensitiveMatcher = SENSITIVE_DATA_PATTERN.matcher(output);
        if (sensitiveMatcher.find()) {
            output = sensitiveMatcher.replaceAll("$1: ***");
        }

        Matcher cardMatcher = CARD_PATTERN.matcher(output);
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        while (cardMatcher.find()) {
            sb.append(output, lastIndex, cardMatcher.start());
            String card = cardMatcher.group().replaceAll("[ -]", "");
            if (card.length() >= 13) {
                // Стандард: 123456******1234
                sb.append(card, 0, 6).append("******").append(card.substring(card.length() - 4));
            } else {
                sb.append("****");
            }
            lastIndex = cardMatcher.end();
        }
        sb.append(output.substring(lastIndex));
        return sb.toString();
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
            return "SYSTEM_INTERNAL";
        }
        return "SYSTEM";
    }
}