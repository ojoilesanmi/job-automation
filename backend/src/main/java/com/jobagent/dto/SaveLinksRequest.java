package com.jobagent.dto;

import java.util.List;

public record SaveLinksRequest(
    List<LinkItem> links
) {
    public record LinkItem(
        String linkType,
        String url,
        String label
    ) {}
}
