package com.ws.backend.repository;

import com.ws.backend.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<AppUser> findByActivationToken(String token);
    @Query("SELECT u FROM AppUser u WHERE u.active = true AND u.lastLoginAt IS NOT NULL AND u.lastLoginAt < :cutoff")
    List<AppUser> findActiveUsersInactiveSince(LocalDateTime cutoff);
}
