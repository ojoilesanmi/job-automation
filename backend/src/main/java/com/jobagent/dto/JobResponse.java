package com.jobagent.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JobResponse(
    UUID id,
    String externalJobId,
    String sourceName,
    String title,
    String company,
    String companyWebsite,
    String description,
    String location,
    String country,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    String currency,
    String remoteType,
    Boolean relocationAvailable,
    Boolean visaSponsorshipSignal,
    String seniority,
    String requiredSkills,
    String preferredSkills,
    Integer experienceYears,
    String employmentType,
    String applicationUrl,
    String atsProvider,
    OffsetDateTime datePosted,
    OffsetDateTime dateDiscovered
) {}
