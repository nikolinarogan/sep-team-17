package com.ws.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Vehicle {

    @Id
    @GeneratedValue
    private Long id;

    private String model;

    private Boolean isAvailable;

    private Double pricePerDay; //moram provjeriti kako ce se raditi sa valutama, zaboravila sam

    private String imageUrl;
}
