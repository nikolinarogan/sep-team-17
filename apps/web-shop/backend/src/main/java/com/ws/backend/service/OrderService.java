package com.ws.backend.service;

import com.ws.backend.dto.OrderRequestDTO;
import com.ws.backend.model.*;
import com.ws.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Order createOrder(OrderRequestDTO request, Long userId) {

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Order order = new Order();
        order.setUser(user);
        order.setCurrency(request.getCurrency() != null ? request.getCurrency() : "EUR");
        order.setOrderStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        if (request.getVehicleId() != null) {
            if (!request.hasDates()) {
                throw new IllegalArgumentException("Start date and end date are required for vehicle orders");
            }
            if (!request.isDateRangeValid()) {
                throw new IllegalArgumentException("End date must be after start date");
            }
            if (request.getStartDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Start date cannot be in the past");
            }

            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + request.getVehicleId()));

            if (vehicle.getAvailable() == null || !vehicle.getAvailable()) {
                throw new IllegalStateException("Vehicle is not available");
            }

            // Proveri da li postoji konfliktna porudžbina (samo CONFIRMED porudžbine)
            boolean isConflict = orderRepository.existsConflictingVehicleOrder(
                    OrderType.VEHICLE,
                    vehicle.getId(),
                    OrderStatus.CONFIRMED,
                    request.getStartDate(),
                    request.getEndDate());
            if (isConflict) {
                throw new IllegalStateException("Vehicle is already booked for this period");
            }

            order.setType(OrderType.VEHICLE);
            order.setVehicle(vehicle);
            order.setStartDate(request.getStartDate());
            order.setEndDate(request.getEndDate());

            order.setPricePerDay(vehicle.getPricePerDay());

            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
            order.setTotalAmount(vehicle.getPricePerDay() * days);

        } else if (request.getInsuranceId() != null) {
            Insurance insurance = insuranceRepository.findById(request.getInsuranceId())
                    .orElseThrow(() -> new RuntimeException("Insurance not found with id: " + request.getInsuranceId()));

            if (insurance.getIsAvailable() == null || !insurance.getIsAvailable()) {
                throw new IllegalStateException("Insurance is not available");
            }

            order.setType(OrderType.INSURANCE);
            order.setInsurance(insurance);

            order.setPrice(insurance.getPrice());

            order.setTotalAmount(insurance.getPrice());

        } else if (request.getEquipmentId() != null) {
            if (!request.hasDates()) {
                throw new IllegalArgumentException("Start date and end date are required for equipment orders");
            }
            if (!request.isDateRangeValid()) {
                throw new IllegalArgumentException("End date must be after start date");
            }
            if (request.getStartDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Start date cannot be in the past");
            }

            Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                    .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + request.getEquipmentId()));

            if (equipment.getAvailable() == null || !equipment.getAvailable()) {
                throw new IllegalStateException("Equipment is not available");
            }

            order.setType(OrderType.EQUIPMENT);
            order.setEquipment(equipment);
            order.setStartDate(request.getStartDate());
            order.setEndDate(request.getEndDate());

            order.setPricePerDay(equipment.getPricePerDay());

            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
            order.setTotalAmount(equipment.getPricePerDay() * days);
        }

        return orderRepository.save(order);
    }
}
