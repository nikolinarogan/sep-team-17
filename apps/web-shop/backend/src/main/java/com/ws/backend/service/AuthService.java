package com.ws.backend.service;

import com.ws.backend.dto.ChangePasswordDTO;
import com.ws.backend.dto.LoginDTO;
import com.ws.backend.dto.LoginResponseDTO;
import com.ws.backend.dto.RegisterDTO;
import com.ws.backend.jwt.JwtService;
import com.ws.backend.model.AppUser;
import com.ws.backend.model.Role;
import com.ws.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSenderService emailSender;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailSenderService emailSender, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.jwtService = jwtService;
    }

    public AppUser registerUser(RegisterDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        AppUser user = new AppUser();
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setVerified(false);
        user.setHasChangedPassword(true);

        String token = UUID.randomUUID().toString();
        user.setActivationToken(token);
        user.setActivationTokenExpiry(LocalDateTime.now().plusHours(24));

        String activationLink = "http://localhost:8080/auth/activate?token=" + token;
        emailSender.sendActivationEmail(user.getEmail(), activationLink, "Activate account");

        return userRepository.save(user);
    }

    @Transactional
    public String activateUser(String token) {
        AppUser user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid activation token"));

        if (user.getVerified()) {
            return "User has already been activated";
        }

        if (user.getActivationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Activation token has expired");
        }

        user.setVerified(true);
        user.setActivationToken(null);
        user.setActivationTokenExpiry(null);

        userRepository.save(user);

        return "User has been activated successfully";
    }

    public AppUser loginCheckCredentials(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElse(null);
    }

    public String generateToken(AppUser user) {
        return jwtService.generateToken(user);
    }

    public AppUser changePassword(ChangePasswordDTO dto) {
        AppUser user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() != Role.ADMIN || user.getHasChangedPassword()) {
            if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Old password is incorrect");
            }
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));

        if (user.getRole() == Role.ADMIN && !user.getHasChangedPassword()) {
            user.setHasChangedPassword(true);
        }

        return userRepository.save(user);
    }
}
