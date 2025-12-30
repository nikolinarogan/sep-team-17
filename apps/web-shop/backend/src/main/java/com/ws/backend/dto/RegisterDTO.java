package com.ws.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDTO {
    private String name;
    private String surname;
    private String email;
    private String password;
}
