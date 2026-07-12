package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String headline,
    String summary,
    String location,
    Integer yearsOfExperience,
    String primaryRole,
    String industries,
    String tonePreference,
    List<SkillDto> skills,
    List<WorkExperienceDto> experiences,
    List<ProjectDto> projects,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public record SkillDto(
        UUID id,
        String skillName,
        String skillType,
        String proficiency,
        Integer yearsUsed
   ) {}

    public record WorkExperienceDto(
        UUID id,
        String company,
        String title,
        String startDate,
        String endDate,
        String description,
        String achievements
   ) {}

    public record ProjectDto(
        UUID id,
        String name,
        String description,
        String technologies,
        String url
    ) {}
}
