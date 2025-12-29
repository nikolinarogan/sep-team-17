package com.ws.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Insurance {

    @Id
    @GeneratedValue
    private Long id;

    private InsuranceType type;

    private Double price;
}
