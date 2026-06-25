package com.jobagent.dto;

public record ApproveMatchResponse(
    JobMatchResponse match,
    CoverLetterResponse coverLetter,
    ApplicationResponse application,
    String coverLetterError,
    String applicationError
) {}
