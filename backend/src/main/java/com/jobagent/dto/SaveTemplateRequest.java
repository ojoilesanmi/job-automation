package com.jobagent.dto;

public record SaveTemplateRequest(
    String name,
    String content,
    String tone,
    String targetRole
) {}
