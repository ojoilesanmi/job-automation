package com.jobagent.dto;

import java.util.Map;

public record DashboardOverviewResponse(
    long totalJobsDiscovered,
    long strongMatches,
    long pendingApproval,
    long applicationsSubmitted,
    long rejectedJobs,
    long interviewInvites,
    long assessmentsReceived,
    double averageMatchScore,
    Map<String, Long> applicationsByCountry,
    Map<String, Long> applicationsByRole
) {}
