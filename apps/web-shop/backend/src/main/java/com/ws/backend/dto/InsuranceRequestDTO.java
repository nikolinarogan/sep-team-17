package com.ws.backend.dto;

import com.ws.backend.model.InsuranceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InsuranceRequestDTO {
    private Double price;
    private InsuranceType type;
    private Boolean isAvailable;

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean getAvailable() {
        return isAvailable;
    }

    public void setAvailable(Boolean available) {
        isAvailable = available;
    }

    public InsuranceType getType() {
        return type;
    }

    public void setType(InsuranceType type) {
        this.type = type;
    }
}

