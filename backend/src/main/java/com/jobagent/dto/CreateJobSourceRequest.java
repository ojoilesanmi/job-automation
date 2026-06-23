package com.jobagent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record CreateJobSourceRequest(
    @NotBlank String name,
    @NotBlank String sourceType,
    String baseUrl,
    Map<String, Object> configJson
) {}
