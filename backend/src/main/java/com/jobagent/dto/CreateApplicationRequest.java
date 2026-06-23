package com.jobagent.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateApplicationRequest(
    @NotNull UUID jobId,
    UUID cvDocumentId,
    UUID coverLetterId,
    String applicationMode
) {}
