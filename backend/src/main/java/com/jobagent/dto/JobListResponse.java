package com.jobagent.dto;

import java.util.List;

public record JobListResponse(
    List<JobResponse> jobs,
    long totalElements,
    int totalPages,
    int currentPage
) {}
