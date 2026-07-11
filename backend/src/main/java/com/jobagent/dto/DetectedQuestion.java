package com.jobagent.dto;

import java.util.List;
import java.util.Map;

public record DetectedQuestion(
    String fieldName,
    String fieldType,
    String label,
    boolean required,
    List<Map<String, String>> options
) {}
