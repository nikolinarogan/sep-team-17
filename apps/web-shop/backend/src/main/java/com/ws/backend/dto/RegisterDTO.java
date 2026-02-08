package com.ws.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Surname is required")
    private String surname;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, message = "Lozinka mora imati najmanje 12 karaktera")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).+$", message = "Lozinka mora sadržati i slova i brojeve")
    private String password;

    public @NotBlank(message = "Password is required") @Size(min = 12, message = "Lozinka mora imati najmanje 12 karaktera") @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).+$", message = "Lozinka mora sadržati i slova i brojeve") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Password is required") @Size(min = 12, message = "Lozinka mora imati najmanje 12 karaktera") @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).+$", message = "Lozinka mora sadržati i slova i brojeve") String password) {
        this.password = password;
    }

    public @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Surname is required") String getSurname() {
        return surname;
    }

    public void setSurname(@NotBlank(message = "Surname is required") String surname) {
        this.surname = surname;
    }

    public @NotBlank(message = "Name is required") String getName() {
        return name;
    }

    public void setName(@NotBlank(message = "Name is required") String name) {
        this.name = name;
    }
}
