package com.jobagent.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ImportJobRequest(
    @NotBlank String url,
    String title,
    String company,
    String description,
    String location,
    String country,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    String currency,
    String remoteType,
    Boolean relocationAvailable,
    String seniority,
    String requiredSkills,
    String applicationUrl
) {}
