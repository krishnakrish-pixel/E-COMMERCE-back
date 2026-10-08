package com.shophub.backend.service;

import com.shophub.backend.config.JwtUtil;
import com.shophub.backend.dto.*;
import com.shophub.backend.entity.User;
import com.shophub.backend.exception.ApiException;
import com.shophub.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttempts;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       LoginAttemptService loginAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.loginAttempts = loginAttempts;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "User already exists with this email");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setFirstName(clean(req.firstName()));
        user.setLastName(clean(req.lastName()));
        user.setRole("user");                      // role can never be chosen by the client
        user.setPhone(clean(req.phone()));
        user.setAddress(clean(req.address()));
        user.setCity(clean(req.city()));
        user.setZipCode(clean(req.zipCode()));
        user.setCountry(clean(req.country()));
        userRepository.save(user);

        return new AuthResponse(jwtUtil.generateToken(user), toDto(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        loginAttempts.assertAllowed(email);                       // 429 while locked

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            loginAttempts.recordFailure(email);                   // unknown e-mails count too (no user guessing)
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        loginAttempts.recordSuccess(email);
        return new AuthResponse(jwtUtil.generateToken(user), toDto(user));
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return toDto(userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found")));
    }

    @Transactional
    public UserDto updateProfile(Long id, ProfileRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (req.firstName() != null) user.setFirstName(clean(req.firstName()));
        if (req.lastName() != null) user.setLastName(clean(req.lastName()));
        if (req.phone() != null) user.setPhone(clean(req.phone()));
        if (req.address() != null) user.setAddress(clean(req.address()));
        if (req.city() != null) user.setCity(clean(req.city()));
        if (req.zipCode() != null) user.setZipCode(clean(req.zipCode()));
        if (req.country() != null) user.setCountry(clean(req.country()));
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long id, PasswordChangeRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        // 400 (not 401) on purpose: the React app signs the user out on any 401
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Your current password is incorrect");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a password you have not used just now");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(AuthService::toDto).toList();
    }

    public static UserDto toDto(User u) {
        return new UserDto(
                String.valueOf(u.getId()),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getRole(),
                u.getPhone(),
                u.getAddress(),
                u.getCity(),
                u.getZipCode(),
                u.getCountry());
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }
}
