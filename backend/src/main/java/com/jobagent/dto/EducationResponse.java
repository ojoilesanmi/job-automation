package com.jobagent.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EducationResponse(
    UUID id,
    String institution,
    String degree,
    String fieldOfStudy,
    LocalDate startDate,
    LocalDate endDate,
    String description
) {}
