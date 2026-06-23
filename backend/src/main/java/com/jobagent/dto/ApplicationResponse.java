package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationResponse(
    UUID id,
    UUID jobId,
    String jobTitle,
    String company,
    UUID cvDocumentId,
    UUID coverLetterId,
    String status,
    String applicationMode,
    OffsetDateTime submittedAt,
    String sourceUrl,
    String notes,
    OffsetDateTime lastFollowUpAt,
    OffsetDateTime nextFollowUpAt,
    OffsetDateTime createdAt
) {}
