package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CoverLetterTemplateResponse(
    UUID id,
    String name,
    String content,
    String tone,
    String targetRole,
    OffsetDateTime createdAt
) {}
