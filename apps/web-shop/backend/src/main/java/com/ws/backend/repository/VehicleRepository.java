package com.ws.backend.repository;

import com.ws.backend.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByIsAvailableTrue();
    Optional<Vehicle> findById(Long id);
}
