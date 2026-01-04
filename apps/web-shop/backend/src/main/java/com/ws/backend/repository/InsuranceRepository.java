package com.ws.backend.repository;
import com.ws.backend.model.Insurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceRepository extends JpaRepository <Insurance, Long> {

    List<Insurance> findByIsAvailableTrue();
    Optional<Insurance> findById(Long id);
}
