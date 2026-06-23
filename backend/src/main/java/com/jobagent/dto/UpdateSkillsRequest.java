package com.jobagent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateSkillsRequest(
    @NotBlank List<SkillItem> skills
) {
    public record SkillItem(
        @NotBlank String skillName,
        String skillType,
        String proficiency,
        Integer yearsUsed
    ) {}
}
