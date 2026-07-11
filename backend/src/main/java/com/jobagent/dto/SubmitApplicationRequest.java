package com.jobagent.dto;

import java.util.Map;
import java.util.UUID;

public record SubmitApplicationRequest(
    UUID applicationId,
    String method,
    Map<String, String> formAnswers
) {}
