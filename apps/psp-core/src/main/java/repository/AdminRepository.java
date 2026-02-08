package repository;

import model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);
    @Query("SELECT a FROM Admin a WHERE a.active = true AND a.lastLoginAt IS NOT NULL AND a.lastLoginAt < :cutoff")
    List<Admin> findActiveAdminsInactiveSince(LocalDateTime cutoff);
}