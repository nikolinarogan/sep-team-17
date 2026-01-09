package com.ws.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ws.backend.model.Vehicle;
import com.ws.backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class VehicleService {

    @Autowired
    private  VehicleRepository vehicleRepository;
    @Autowired
    private Cloudinary cloudinary;

    public Vehicle addVehicle(String model, Boolean isAvailable, Double pricePerDay, MultipartFile imageFile) throws IOException {
        Vehicle vehicle = new Vehicle();
        vehicle.setModel(model);
        vehicle.setAvailable(isAvailable);
        vehicle.setPricePerDay(pricePerDay);

        // Upload image to Cloudinary
        if (imageFile != null && !imageFile.isEmpty()) {
            Map uploadResult = cloudinary.uploader().upload(imageFile.getBytes(), ObjectUtils.emptyMap());
            String imageUrl = uploadResult.get("secure_url").toString();
            vehicle.setImageUrl(imageUrl);
        }

        return vehicleRepository.save(vehicle);
    }

    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByIsAvailableTrue();
    }

    public Vehicle updateVehicle(Long id, Vehicle updatedVehicle, MultipartFile imageFile) throws IOException {
        Vehicle vehicle = getVehicleById(id);
        vehicle.setModel(updatedVehicle.getModel());
        vehicle.setPricePerDay(updatedVehicle.getPricePerDay());
        vehicle.setAvailable(updatedVehicle.getAvailable());

        if (imageFile != null && !imageFile.isEmpty()) {
            Map uploadResult = cloudinary.uploader().upload(imageFile.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "vehicles",
                            "public_id", "vehicle_" + updatedVehicle.getModel().replaceAll("\\s+", "_")
                    ));
            vehicle.setImageUrl((String) uploadResult.get("secure_url"));
        }

        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(Long id) {
        Vehicle vehicle = getVehicleById(id);
        vehicle.setAvailable(false);
        vehicleRepository.save(vehicle);
    }
}
