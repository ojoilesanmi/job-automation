package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record JobSourceResponse(
    UUID id,
    String name,
    String sourceType,
    String baseUrl,
    Boolean enabled,
    Map<String, Object> configJson,
    OffsetDateTime createdAt
) {}
