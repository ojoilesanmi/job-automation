package com.jobagent.dto;

import java.util.List;

public record CoverLetterListResponse(
    List<CoverLetterResponse> coverLetters
) {}
