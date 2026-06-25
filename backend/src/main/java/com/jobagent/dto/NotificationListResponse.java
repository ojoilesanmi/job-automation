package com.jobagent.dto;

import java.util.List;

public record NotificationListResponse(
    List<NotificationResponse> notifications,
    long totalElements,
    int totalPages,
    int currentPage
) {}
