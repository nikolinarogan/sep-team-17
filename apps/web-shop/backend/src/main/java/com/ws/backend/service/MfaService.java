package com.ws.backend.service;

import com.ws.backend.model.AppUser;
import com.ws.backend.repository.UserRepository;
import com.ws.backend.tools.AuditLogger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MfaService {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 5;

    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final AuditLogger auditLogger;

    public MfaService(UserRepository userRepository, EmailSenderService emailSenderService,
                      AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.emailSenderService = emailSenderService;
        this.auditLogger = auditLogger;
    }

    public void generateAndSendCode(AppUser user) {
        String code = generateSixDigitCode();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);

        user.setMfaCode(code);
        user.setMfaExpiry(expiry);
        userRepository.save(user);

        emailSenderService.sendMfaEmail(user.getEmail(), code);
        auditLogger.logEvent("MFA_CODE_SENT", "SUCCESS", "Email: " + user.getEmail());
    }

    public Optional<AppUser> verifyCode(String email, String code) {
        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            auditLogger.logSecurityAlert("MFA_VERIFICATION_FAILURE", "User not found: " + email);
            return Optional.empty();
        }

        String storedCode = user.getMfaCode();
        LocalDateTime expiry = user.getMfaExpiry();

        user.setMfaCode(null);
        user.setMfaExpiry(null);
        userRepository.save(user);

        if (storedCode == null || storedCode.isBlank() || expiry == null) {
            auditLogger.logSecurityAlert("MFA_VERIFICATION_FAILURE", "No valid MFA pending for: " + email);
            return Optional.empty();
        }

        if (LocalDateTime.now().isAfter(expiry)) {
            auditLogger.logSecurityAlert("MFA_VERIFICATION_FAILURE", "MFA expired for: " + email);
            return Optional.empty();
        }

        if (!storedCode.equals(code)) {
            auditLogger.logSecurityAlert("MFA_VERIFICATION_FAILURE", "Invalid code for: " + email);
            return Optional.empty();
        }

        auditLogger.logEvent("MFA_VERIFICATION_SUCCESS", "SUCCESS", "User: " + email);
        return Optional.of(user);
    }

    private String generateSixDigitCode() {
        int min = (int) Math.pow(10, CODE_LENGTH - 1);
        int max = (int) Math.pow(10, CODE_LENGTH) - 1;
        return String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
    }
}
