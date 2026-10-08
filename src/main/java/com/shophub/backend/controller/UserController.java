package com.shophub.backend.controller;

import com.shophub.backend.dto.PasswordChangeRequest;
import com.shophub.backend.dto.ProfileRequest;
import com.shophub.backend.dto.UserDto;
import com.shophub.backend.entity.User;
import com.shophub.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /** Currently logged-in user. */
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal User user) {
        return authService.getUser(user.getId());
    }

    /** Update own profile (name, phone, address ...). Email and role cannot be changed here. */
    @PutMapping("/me")
    public UserDto updateMe(@AuthenticationPrincipal User user, @RequestBody ProfileRequest request) {
        return authService.updateProfile(user.getId(), request);
    }

    /** Change own password. */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(user.getId(), request);
        return ResponseEntity.noContent().build();
    }

    /** Admin only (see SecurityConfig): all registered accounts for the Customers tab. */
    @GetMapping
    public List<UserDto> all() {
        return authService.getAllUsers();
    }
}
