package com.jobagent.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FollowUpResponse(
    UUID applicationId,
    OffsetDateTime nextFollowUpAt,
    OffsetDateTime lastFollowUpAt
) {}
