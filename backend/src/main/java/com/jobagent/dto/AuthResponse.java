package com.jobagent.dto;

import java.util.List;

public record AuthResponse(
    String token,
    String userId,
    String email,
    String firstName,
    String lastName,
    List<String> roles,
    long expiresIn
) {}
