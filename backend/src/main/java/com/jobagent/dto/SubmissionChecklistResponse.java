package com.jobagent.dto;

import java.util.List;

public record SubmissionChecklistResponse(
    boolean ready,
    List<CheckItem> items
) {
    public record CheckItem(String check, boolean passed, String message) {}
}
