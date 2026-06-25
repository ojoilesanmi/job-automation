package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID userId,
    String userEmail,
    String action,
    String entityType,
    UUID entityId,
    Map<String, Object> metadata,
    OffsetDateTime createdAt
) {}
