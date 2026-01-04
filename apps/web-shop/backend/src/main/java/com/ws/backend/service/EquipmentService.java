package com.ws.backend.service;
import com.ws.backend.model.Equipment;
import com.ws.backend.model.EquipmentType;
import com.ws.backend.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;

@Service
public class EquipmentService {

    @Autowired
    private EquipmentRepository equipmentRepository;

    public Equipment addEquipment(Double pricePerDay, EquipmentType type, Boolean isAvailable) throws IOException {
        Equipment equipment = new Equipment();
        equipment.setEquipmentType(type);
        equipment.setPricePerDay(pricePerDay);
        equipment.setAvailable(isAvailable);

        return equipmentRepository.save(equipment);
    }

    public Equipment getEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
    }

    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    public List<Equipment> getAvailableEquipment() {
        return equipmentRepository.findByIsAvailableTrue();
    }

    public Equipment updateEquipment(Long id, Equipment updatedEquipment) throws IOException {
        Equipment equipment = getEquipmentById(id);
        equipment.setEquipmentType(updatedEquipment.getEquipmentType());
        equipment.setPricePerDay(updatedEquipment.getPricePerDay());
        equipment.setAvailable(updatedEquipment.getAvailable());

        return equipmentRepository.save(equipment);
    }

    public void deleteEquipment(Long id) {
        Equipment equipment = getEquipmentById(id);
        equipmentRepository.delete(equipment);
    }
}
