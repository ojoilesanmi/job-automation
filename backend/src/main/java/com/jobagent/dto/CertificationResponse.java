package com.jobagent.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CertificationResponse(
    UUID id,
    String name,
    String issuingOrg,
    LocalDate issueDate,
    LocalDate expiryDate,
    String credentialUrl
) {}
