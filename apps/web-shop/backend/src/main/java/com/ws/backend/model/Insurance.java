package com.ws.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Insurance {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    @Enumerated(EnumType.STRING)
    private InsuranceType type;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Boolean isAvailable;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public InsuranceType getType() {
        return type;
    }

    public void setType(InsuranceType type) {
        this.type = type;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }
    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}
