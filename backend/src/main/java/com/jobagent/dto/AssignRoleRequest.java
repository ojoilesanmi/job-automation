package com.jobagent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record AssignRoleRequest(
    @NotBlank String roleName
) {}
