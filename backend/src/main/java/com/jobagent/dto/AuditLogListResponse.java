package com.jobagent.dto;

import java.util.List;

public record AuditLogListResponse(
    List<AuditLogResponse> logs,
    long totalElements,
    int totalPages,
    int currentPage
) {}
