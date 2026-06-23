package com.jobagent.dto;

import java.math.BigDecimal;

public record UpdatePreferencesRequest(
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
    Boolean approvalRequired
) {}
