package com.ws.backend.controller;

import com.ws.backend.dto.ChangePasswordDTO;
import com.ws.backend.dto.LoginDTO;
import com.ws.backend.dto.LoginResponseDTO;
import com.ws.backend.dto.RegisterDTO;
import com.ws.backend.model.AppUser;
import com.ws.backend.model.Role;
import com.ws.backend.repository.UserRepository;
import com.ws.backend.service.AuthService;
import com.ws.backend.service.SessionActivityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "https://localhost:4200")
public class AuthController {

    private final AuthService userService;
    private final UserRepository userRepository;
    private final SessionActivityService sessionActivityService;


    public AuthController(AuthService userService, UserRepository userRepository, SessionActivityService sessionActivityService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.sessionActivityService = sessionActivityService;
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
        if (user.isActive() == false) {
            return ResponseEntity.badRequest()
                    .body(new LoginResponseDTO("User account has been deactivated!", null, false));
        }
        // First-time admin login
        if (user.getRole() == Role.ADMIN && !user.getHasChangedPassword()) {
            return ResponseEntity.ok(
                    new LoginResponseDTO("Admin must change password first", null, true)
            );
        }

        sessionActivityService.updateActivity(user.getId());

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

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(false);
                    userRepository.save(user);

                    return ResponseEntity.ok("Korisnikov nalog je uspešno deaktiviran.");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}