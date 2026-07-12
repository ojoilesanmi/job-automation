package com.jobagent.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserPreferencesResponse(
    UUID id,
    String targetRoles,
    String targetSeniority,
    String preferredSkills,
    String mustHaveSkills,
    String niceToHaveSkills,
    Boolean remoteFirst,
    Boolean relocationFriendly,
    String preferredCountries,
    String excludedCountries,
    String excludedCompanies,
    BigDecimal remoteMinSalary,
    BigDecimal relocationMinSalary,
    BigDecimal nigeriaMinSalary,
    BigDecimal minimumRemoteFitScore,
    BigDecimal minimumRelocationFitScore,
    BigDecimal minimumNigeriaFitScore,
    Integer maxApplicationsPerDay,
    Boolean approvalRequired,
    String autoRejectRules,
    String excludedJobLevels,
    String excludedIndustries,
    OffsetDateTime updatedAt
) {}
