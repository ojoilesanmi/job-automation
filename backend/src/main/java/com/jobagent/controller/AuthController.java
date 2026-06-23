package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.AuthUserDetails;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Login successful", authService.login(request)));
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
