package service;

import model.Admin;
import org.springframework.stereotype.Service;
import repository.AdminRepository;
import tools.AuditLogger;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MfaService {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 5;

    private final AdminRepository adminRepository;
    private final EmailService emailService;
    private final AuditLogger auditLogger;

    public MfaService(AdminRepository adminRepository, EmailService emailService, AuditLogger auditLogger) {
        this.adminRepository = adminRepository;
        this.emailService = emailService;
        this.auditLogger = auditLogger;
    }

    public void generateAndSendCode(Admin admin) {
        String code = generateSixDigitCode();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);

        admin.setMfaCode(code);
        admin.setMfaExpiry(expiry);
        adminRepository.save(admin);

        emailService.sendMfaEmail(admin.getUsername(), code);
        auditLogger.logEvent("MFA_CODE_SENT", "SUCCESS", "Email: " + admin.getUsername());
    }

    /**
     * Verifies the MFA code. Clears code and expiry from DB immediately after check (regardless of outcome)
     * to prevent replay attacks per PCI DSS 8.5.1.
     */
    public Optional<Admin> verifyCode(String email, String code) {
        Admin admin = adminRepository.findByUsername(email).orElse(null);
        if (admin == null) {
            auditLogger.logSecurityAlert("MFA_VERIFICATION_FAILURE", "Admin not found: " + email);
            return Optional.empty();
        }

        String storedCode = admin.getMfaCode();
        LocalDateTime expiry = admin.getMfaExpiry();

        // Clear code and expiry immediately (before validation) to prevent replay attacks
        admin.setMfaCode(null);
        admin.setMfaExpiry(null);
        adminRepository.save(admin);

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
        return Optional.of(admin);
    }

    private String generateSixDigitCode() {
        int min = (int) Math.pow(10, CODE_LENGTH - 1);
        int max = (int) Math.pow(10, CODE_LENGTH) - 1;
        return String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
    }
}
