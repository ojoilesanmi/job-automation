package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    String email,
    String fullName,
    Set<String> roles,
    OffsetDateTime createdAt
) {}
