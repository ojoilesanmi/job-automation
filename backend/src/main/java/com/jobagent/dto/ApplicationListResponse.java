package com.jobagent.dto;

import java.util.List;

public record ApplicationListResponse(
    List<ApplicationResponse> applications,
    long totalElements,
    int totalPages,
    int currentPage
) {}
