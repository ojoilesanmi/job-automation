package com.jobagent.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GenerateCoverLetterRequest(
    @NotNull UUID jobId,
    UUID cvDocumentId,
    String tone
) {}
