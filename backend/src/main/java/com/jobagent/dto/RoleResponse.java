package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record RoleResponse(
    UUID id,
    String name,
    String description,
    Set<String> permissions,
    OffsetDateTime createdAt
) {}
