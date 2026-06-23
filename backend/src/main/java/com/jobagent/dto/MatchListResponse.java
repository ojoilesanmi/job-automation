package com.jobagent.dto;

import java.util.List;

public record MatchListResponse(
    List<JobMatchResponse> matches,
    long totalElements,
    int totalPages,
    int currentPage
) {}
