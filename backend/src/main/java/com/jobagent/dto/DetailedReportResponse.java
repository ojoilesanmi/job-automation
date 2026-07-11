package com.jobagent.dto;

import java.util.List;
import java.util.Map;

public record DetailedReportResponse(
    long totalJobs,
    long totalMatches,
    long totalApplications,
    long totalSubmitted,
    long totalInterviews,
    long totalOffers,
    double averageMatchScore,
    double responseRate,
    Map<String, Long> matchesByStatus,
    Map<String, Long> applicationsByStatus,
    List<CountryReport> applicationsByCountry,
    List<SourceReport> applicationsBySource,
    List<WeeklyReport> weeklyTrend
) {
    public record CountryReport(String country, long count) {}
    public record SourceReport(String sourceName, long count) {}
    public record WeeklyReport(String week, long matches, long applications, long submitted) {}
}
