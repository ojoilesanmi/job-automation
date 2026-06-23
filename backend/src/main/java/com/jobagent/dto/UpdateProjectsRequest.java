package com.jobagent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateProjectsRequest(
    @NotBlank List<ProjectItem> projects
) {
    public record ProjectItem(
        @NotBlank String name,
        String description,
        String technologies,
        String url
    ) {}
}
