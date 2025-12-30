package com.ws.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class AppUser  {

    @Id
    @GeneratedValue
    private Long id;

    private String email; // will be used as username

    private String password;

    private String name;

    private String surname;

    private Boolean isVerified = false;

    private String activationToken;

    private LocalDateTime activationTokenExpiry;

    private Boolean hasChangedPassword = false; // admin must change default password first time he logs in

    private Role role;
}
