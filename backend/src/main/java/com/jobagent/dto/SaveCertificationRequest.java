package com.jobagent.dto;

import java.time.LocalDate;
import java.util.List;

public record SaveCertificationRequest(
    List<CertificationItem> certifications
) {
    public record CertificationItem(
        String name,
        String issuingOrg,
        LocalDate issueDate,
        LocalDate expiryDate,
        String credentialUrl
    ) {}
}
