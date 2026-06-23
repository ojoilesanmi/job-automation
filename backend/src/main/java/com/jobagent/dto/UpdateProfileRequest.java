package com.jobagent.dto;

public record UpdateProfileRequest(
    String headline,
    String summary,
    String location,
    Integer yearsOfExperience,
    String primaryRole
) {}
