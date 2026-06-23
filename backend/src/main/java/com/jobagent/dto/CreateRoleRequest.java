package com.jobagent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record CreateRoleRequest(
    @NotBlank String name,
    String description,
    Set<String> permissions
) {}
