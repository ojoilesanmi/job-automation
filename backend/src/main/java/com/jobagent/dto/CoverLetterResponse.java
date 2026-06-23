package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CoverLetterResponse(
    UUID id,
    UUID jobId,
    String jobTitle,
    String company,
    UUID cvDocumentId,
    String content,
    Integer version,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
