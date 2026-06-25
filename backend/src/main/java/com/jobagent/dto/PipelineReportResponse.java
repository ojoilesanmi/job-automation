package com.jobagent.dto;

import java.util.List;
import java.util.Map;

public record PipelineReportResponse(
    Map<String, Long> byStatus,
    List<CountryCount> byCountry,
    List<SourceCount> bySource,
    long totalApplications,
    long thisWeekApplications,
    long thisMonthApplications
) {
    public record CountryCount(String country, long count) {}
    public record SourceCount(String sourceId, String sourceName, long count) {}
}
