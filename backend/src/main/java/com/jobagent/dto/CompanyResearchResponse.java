package com.jobagent.dto;

public record CompanyResearchResponse(
    String companyName,
    String summary,
    String companySize,
    String industry,
    String techStack,
    String glassdoorRating,
    String linkedInUrl
) {}
