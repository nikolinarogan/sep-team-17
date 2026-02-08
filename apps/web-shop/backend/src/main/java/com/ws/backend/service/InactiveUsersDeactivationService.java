package com.ws.backend.service;

import com.ws.backend.model.AppUser;
import com.ws.backend.repository.UserRepository;
import com.ws.backend.tools.AuditLogger;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InactiveUsersDeactivationService {
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    public InactiveUsersDeactivationService(UserRepository userRepository, AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    @Scheduled(cron = "0 0 2 * * ?")  // Svaki dan u 02:00
    @Transactional
    public void deactivateInactiveUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        List<AppUser> inactiveUsers = userRepository.findActiveUsersInactiveSince(cutoff);

        for (AppUser user : inactiveUsers) {
            user.setActive(false);
            userRepository.save(user);
            auditLogger.logEvent("USER_DEACTIVATED_INACTIVE", "SUCCESS",
                    "Deaktiviran korisnik (90+ dana neaktivnosti): " + user.getName() + " " + user.getSurname());
        }
    }
}
