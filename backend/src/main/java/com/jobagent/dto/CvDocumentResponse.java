package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CvDocumentResponse(
    UUID id,
    String fileName,
    String fileUrl,
    String parsedText,
    String versionName,
    Boolean isDefault,
    OffsetDateTime createdAt
) {}
