package com.ws.backend.controller;

import com.ws.backend.dto.ChangePasswordDTO;
import com.ws.backend.dto.LoginDTO;
import com.ws.backend.dto.LoginResponseDTO;
import com.ws.backend.dto.RegisterDTO;
import com.ws.backend.model.AppUser;
import com.ws.backend.model.Role;
import com.ws.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService userService;

    public AuthController(AuthService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO request) {
        try {
            AppUser savedUser = userService.registerUser(request);
            return ResponseEntity.ok("User registered with ID: " + savedUser.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while registering the user");
        }
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activateUser(@RequestParam("token") String token) {
        try {
            String result = userService.activateUser(token);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginDTO request) {

        AppUser user = userService.loginCheckCredentials(request.getEmail(), request.getPassword());

        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(new LoginResponseDTO("Invalid email or password", null, false));
        }

        // First-time admin login
        if (user.getRole() == Role.ADMIN && !user.getHasChangedPassword()) {
            return ResponseEntity.ok(
                    new LoginResponseDTO("Admin must change password first", null, true)
            );
        }


        String token = userService.generateToken(user);

        return ResponseEntity.ok(
                new LoginResponseDTO("Login successful", token, false)
        );
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDTO dto) {
        try {
            AppUser updatedUser = userService.changePassword(dto);
            return ResponseEntity.ok("Password changed successfully for " + updatedUser.getEmail());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error changing password");
        }
    }
}