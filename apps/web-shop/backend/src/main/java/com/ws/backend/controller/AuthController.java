package com.ws.backend.controller;

import com.ws.backend.dto.ChangePasswordDTO;
import com.ws.backend.dto.LoginDTO;
import com.ws.backend.dto.LoginResponseDTO;
import com.ws.backend.dto.RegisterDTO;
import com.ws.backend.dto.VerifyMfaRequestDTO;
import com.ws.backend.model.AppUser;
import com.ws.backend.model.Role;
import com.ws.backend.repository.UserRepository;
import com.ws.backend.service.AuthService;
import com.ws.backend.service.LoginAttemptService;
import com.ws.backend.service.MfaService;
import com.ws.backend.service.SessionActivityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "https://localhost:4200")
public class AuthController {

    private final AuthService userService;
    private final UserRepository userRepository;
    private final SessionActivityService sessionActivityService;
    private final LoginAttemptService loginAttemptService;
    private final MfaService mfaService;

    public AuthController(AuthService userService, UserRepository userRepository,
                          SessionActivityService sessionActivityService,
                          LoginAttemptService loginAttemptService,
                          MfaService mfaService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.sessionActivityService = sessionActivityService;
        this.loginAttemptService = loginAttemptService;
        this.mfaService = mfaService;
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
    public ResponseEntity<?> login(@RequestBody LoginDTO request) {
        String email = request.getEmail();
        if (email != null && !email.isBlank()) {
            if (loginAttemptService.isLockedOut(email)) {
                long remaining = loginAttemptService.getRemainingLockoutMinutes(email);
                return ResponseEntity.status(429)
                        .body(new LoginResponseDTO("Prijava onemogućena. Previše neuspešnih pokušaja. Pokušajte ponovo za " + remaining + " minuta.", null, false));
            }
        }

        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            if (email != null && !email.isBlank()) {
                loginAttemptService.recordFailedAttempt(email);
            }
            return ResponseEntity.badRequest()
                    .body(new LoginResponseDTO("Invalid email or password", null, false));
        }

        if (!user.isActive()) {
            return ResponseEntity.badRequest()
                    .body(new LoginResponseDTO("User account has been deactivated!", null, false));
        }
        // First-time admin login
        if (user.getRole() == Role.ADMIN && Boolean.FALSE.equals(user.getHasChangedPassword())) {
            // Still verify password before redirecting to change password
            if (userService.verifyPassword(email, request.getPassword()).isEmpty()) {
                if (email != null && !email.isBlank()) {
                    loginAttemptService.recordFailedAttempt(email);
                }
                return ResponseEntity.badRequest()
                        .body(new LoginResponseDTO("Invalid email or password", null, false));
            }
            loginAttemptService.clearAttempts(email);
            return ResponseEntity.ok(
                    new LoginResponseDTO("Admin must change password first", null, true)
            );
        }

        // Admin: MFA required (does not update lastLoginAt until verify-mfa)
        if (user.getRole() == Role.ADMIN) {
            if (userService.verifyPassword(email, request.getPassword()).isEmpty()) {
                if (email != null && !email.isBlank()) {
                    loginAttemptService.recordFailedAttempt(email);
                }
                return ResponseEntity.badRequest()
                        .body(new LoginResponseDTO("Invalid email or password", null, false));
            }
            loginAttemptService.clearAttempts(email);
            mfaService.generateAndSendCode(user);
            return ResponseEntity.ok(Map.of("status", "MFA_REQUIRED", "email", user.getEmail()));
        }

        // Ordinary user: no MFA, standard login
        AppUser verified = userService.loginCheckCredentials(email, request.getPassword());
        if (verified == null) {
            if (email != null && !email.isBlank()) {
                loginAttemptService.recordFailedAttempt(email);
            }
            return ResponseEntity.badRequest()
                    .body(new LoginResponseDTO("Invalid email or password", null, false));
        }
        loginAttemptService.clearAttempts(email);
        sessionActivityService.updateActivity(verified.getId());
        String token = userService.generateToken(verified);
        return ResponseEntity.ok(new LoginResponseDTO("Login successful", token, false));
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<?> verifyMfa(@Valid @RequestBody VerifyMfaRequestDTO request) {
        var userOpt = mfaService.verifyCode(request.getEmail(), request.getCode());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Neispravan ili istekao kod. Pokušajte ponovo.");
        }

        AppUser user = userOpt.get();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        sessionActivityService.updateActivity(user.getId());
        String token = userService.generateToken(user);

        return ResponseEntity.ok(Map.of(
                "message", "Uspešna prijava.",
                "token", token
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
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