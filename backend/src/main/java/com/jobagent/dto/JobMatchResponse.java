package com.jobagent.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JobMatchResponse(
    UUID id,
    UUID jobId,
    String jobTitle,
    String company,
    BigDecimal fitScore,
    BigDecimal skillsScore,
    BigDecimal experienceScore,
    BigDecimal roleScore,
    BigDecimal locationScore,
    BigDecimal salaryScore,
    String matchedSkills,
    String missingSkills,
    String reasonsToApply,
    String reasonsToSkip,
    String riskFlags,
    String status,
    OffsetDateTime createdAt
) {}
