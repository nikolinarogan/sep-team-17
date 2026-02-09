package service;

import jakarta.transaction.Transactional;
import model.Admin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import repository.AdminRepository;
import tools.AuditLogger;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InactiveAdminDeactivationService {
    private final AdminRepository adminRepository;
    private final AuditLogger auditLogger;

    public InactiveAdminDeactivationService(AdminRepository adminRepository,
                                            AuditLogger auditLogger) {
        this.adminRepository = adminRepository;
        this.auditLogger = auditLogger;
    }

    @Scheduled(cron = "0 0 2 * * ?")  // Svaki dan u 02:00
    @Transactional
    public void deactivateInactiveAdmins() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        List<Admin> inactiveAdmins = adminRepository.findActiveAdminsInactiveSince(cutoff);

        for (Admin admin : inactiveAdmins) {
            admin.setActive(false);
            adminRepository.save(admin);
            auditLogger.logEvent("ADMIN_DEACTIVATED_INACTIVE", "SUCCESS",
                    "Deaktiviran admin (90+ dana neaktivnosti): " + admin.getUsername());
        }
    }
}
