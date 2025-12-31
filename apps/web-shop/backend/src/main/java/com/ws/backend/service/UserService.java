package com.ws.backend.service;

import com.ws.backend.dto.RegisterDTO;
import com.ws.backend.model.AppUser;
import com.ws.backend.model.Role;
import com.ws.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void registerUser(RegisterDTO registerDTO) {

        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String activationToken = UUID.randomUUID().toString();

        String hashedPassword = passwordEncoder.encode(registerDTO.getPassword());

        AppUser appUser = new AppUser();
        appUser.setEmail(registerDTO.getEmail());
        appUser.setPassword(hashedPassword);
        appUser.setName(registerDTO.getName());
        appUser.setSurname(registerDTO.getSurname());
        appUser.setIsVerified(false);
        appUser.setActivationToken(activationToken);
        appUser.setActivationTokenExpiry(LocalDateTime.now().plusHours(24));
        appUser.setRole(Role.USER);

        try {
            userRepository.save(appUser);

            emailService.sendActivationEmail(registerDTO.getEmail(), activationToken);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Email already exists", e);
        }
    }

    @Transactional
    public void activateUser(String token) {
        AppUser user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid activation token"));

        if (user.getActivationTokenExpiry() == null ||
                user.getActivationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Activation token has expired");
        }

        if (user.getIsVerified()) {
            throw new IllegalArgumentException("Account is already activated");
        }

        user.setIsVerified(true);
        user.setActivationToken(null);
        user.setActivationTokenExpiry(null);

        userRepository.save(user);
    }

}
