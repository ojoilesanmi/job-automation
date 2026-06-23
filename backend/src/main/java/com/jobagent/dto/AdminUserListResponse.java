package com.jobagent.dto;

import java.util.List;

public record AdminUserListResponse(
    List<AdminUserResponse> users,
    long totalElements,
    int totalPages,
    int currentPage
) {}
