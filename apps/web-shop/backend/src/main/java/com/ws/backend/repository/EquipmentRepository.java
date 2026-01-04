package com.ws.backend.repository;

import com.ws.backend.model.Equipment;
import com.ws.backend.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository <Equipment, Long> {

    List<Equipment> findByIsAvailableTrue();
    Optional<Equipment> findById(Long id);
}
