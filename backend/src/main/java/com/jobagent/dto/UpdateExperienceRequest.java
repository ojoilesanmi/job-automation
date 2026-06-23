package com.jobagent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateExperienceRequest(
    @NotBlank List<ExperienceItem> experiences
) {
    public record ExperienceItem(
        @NotBlank String company,
        @NotBlank String title,
        String startDate,
        String endDate,
        String description,
        String achievements
    ) {}
}
