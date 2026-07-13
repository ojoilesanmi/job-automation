package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.AuthUserDetails;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.AuthService;
import com.jobagent.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<BaseResponse<AuthResponse>> googleLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        return ResponseEntity.ok(BaseResponse.success("Google login successful",
                authService.loginWithGoogle(code)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<Void>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(BaseResponse.error("VALIDATION_ERROR", "Email is required"));
        }
        try {
            UUID userId = authService.getUserIdByEmail(email);
            passwordResetService.requestReset(userId);
        } catch (Exception ignored) {
            // Always return a generic success response to avoid account enumeration.
        }
        return ResponseEntity.ok(BaseResponse.success("Password reset email sent", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<Void>> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        if (email == null || email.isBlank() || token == null || token.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(BaseResponse.error("VALIDATION_ERROR", "Email, token, and newPassword are required"));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(BaseResponse.error("VALIDATION_ERROR", "Password must be at least 8 characters"));
        }
        UUID userId = authService.getUserIdByEmail(email);
        passwordResetService.resetPassword(userId, token, newPassword);
        return ResponseEntity.ok(BaseResponse.success("Password reset successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<BaseResponse<Map<String, Object>>> me() {
        AuthUserDetails details = SecurityUtils.getCurrentUserDetails();
        Map<String, Object> data = Map.of(
            "userId", details.getUserId().toString(),
            "email", details.getUsername(),
            "firstName", details.getUser().getFullName().split(" ", 2)[0],
            "lastName", details.getUser().getFullName().contains(" ") ? details.getUser().getFullName().split(" ", 2)[1] : "",
            "roles", List.copyOf(details.getRoleNames()),
            "permissions", List.copyOf(details.getPermissionNames())
        );
        return ResponseEntity.ok(BaseResponse.success("User details", data));
    }
}
