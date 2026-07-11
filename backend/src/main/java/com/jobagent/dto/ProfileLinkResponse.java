package com.jobagent.dto;

import java.util.UUID;

public record ProfileLinkResponse(
    UUID id,
    String linkType,
    String url,
    String label
) {}
