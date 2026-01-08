package com.ws.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderRequestDTO {

    private Long vehicleId;
    
    private Long insuranceId;
    
    private Long equipmentId;


    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;
    
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g., EUR, USD)")
    private String currency = "EUR";

    public boolean hasDates() {
        return startDate != null && endDate != null;
    }

    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return endDate.isAfter(startDate);
    }

    public String getOrderType() {
        if (vehicleId != null) {
            return "VEHICLE";
        } else if (insuranceId != null) {
            return "INSURANCE";
        } else if (equipmentId != null) {
            return "EQUIPMENT";
        }
        return null;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getInsuranceId() {
        return insuranceId;
    }

    public void setInsuranceId(Long insuranceId) {
        this.insuranceId = insuranceId;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

