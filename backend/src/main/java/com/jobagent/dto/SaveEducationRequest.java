package com.jobagent.dto;

import java.time.LocalDate;
import java.util.List;

public record SaveEducationRequest(
    List<EducationItem> education
) {
    public record EducationItem(
        String institution,
        String degree,
        String fieldOfStudy,
        LocalDate startDate,
        LocalDate endDate,
        String description
    ) {}
}
