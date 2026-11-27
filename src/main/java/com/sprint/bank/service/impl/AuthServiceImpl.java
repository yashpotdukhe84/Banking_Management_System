package com.sprint.bank.service.impl;

import com.sprint.bank.dto.AuthResponse;
import com.sprint.bank.dto.LoginRequest;
import com.sprint.bank.dto.RegisterRequest;
import com.sprint.bank.repository.UserRepository;
import com.sprint.bank.security.JwtUtil;
import com.sprint.bank.service.AuthService;
import com.sprint.bank.shared.entity.CustomerProfile;
import com.sprint.bank.shared.entity.User;
import com.sprint.bank.shared.enums.Role;
import com.sprint.bank.shared.exception.DuplicateResourceException;
import com.sprint.bank.shared.exception.InvalidCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Simple, straightforward implementation of register, login, and logout. */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = new User(email, passwordEncoder.encode(request.getPassword()), Role.CUSTOMER);

        CustomerProfile profile = new CustomerProfile();
        profile.setFullName(request.getFullName().trim());
        profile.setPhone(request.getPhone().trim());
        profile.setAddress(request.getAddress().trim());
        if (request.getPanNumber() != null && !request.getPanNumber().isBlank()) {
            profile.setPan(request.getPanNumber().trim());
        }
        profile.setUser(user);
        user.setCustomerProfile(profile);

        try {
            // saveAndFlush triggers unique-constraint checks immediately inside this try block
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Email, phone, or PAN is already registered");
        }

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    @Override
    public void logout(String token) {
        jwtUtil.blacklistToken(token);
        SecurityContextHolder.clearContext();
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String fullName = user.getCustomerProfile() != null ? user.getCustomerProfile().getFullName() : null;
        return new AuthResponse(
                token,
                jwtUtil.getExpirationSeconds(),
                user.getEmail(),
                user.getRole().name(),
                fullName
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
