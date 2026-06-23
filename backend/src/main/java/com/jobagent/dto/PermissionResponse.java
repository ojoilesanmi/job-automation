package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PermissionResponse(
    UUID id,
    String name,
    String description,
    OffsetDateTime createdAt
) {}
