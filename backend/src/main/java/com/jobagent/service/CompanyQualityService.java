package com.jobagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CompanyQualityService {

    private static final Set<String> KNOWN_GOOD_COMPANIES = Set.of(
            "google", "microsoft", "apple", "amazon", "meta", "netflix", "spotify",
            "stripe", "airbnb", "shopify", "twilio", "cloudflare", "datadog",
            "snowflake", "databricks", "figma", "notion", "vercel", "supabase",
            "linear", "plausible", "posthog", "grafana", "gitlab", "github"
    );

    private static final Set<String> KNOWN_SCAM_KEYWORDS = Set.of(
            "wire transfer", "western union", "upfront fee", "too good to be true",
            "guaranteed income", "no experience needed", "work from home guaranteed"
    );

    public double scoreCompany(String company, String description, Double salaryMin, Double salaryMax) {
        if (company == null || company.isBlank()) return 50.0;

        double score = 50.0;
        String lower = company.toLowerCase().trim();

        if (KNOWN_GOOD_COMPANIES.contains(lower)) {
            score += 30;
        }

        if (description != null) {
            String descLower = description.toLowerCase();
            for (String keyword : KNOWN_SCAM_KEYWORDS) {
                if (descLower.contains(keyword)) {
                    score -= 40;
                    break;
                }
            }
        }

        if (salaryMin != null && salaryMin > 80000) score += 10;
        if (salaryMax != null && salaryMax > 150000) score += 10;

        if (description != null && description.length() > 200) score += 5;

        return Math.max(0, Math.min(100, score));
    }

    public Map<String, Object> getQualityBreakdown(String company, String description, Double salaryMin, Double salaryMax) {
        double total = scoreCompany(company, description, salaryMin, salaryMax);
        String tier;
        if (total >= 80) tier = "high";
        else if (total >= 50) tier = "medium";
        else tier = "low";

        return Map.of(
                "score", total,
                "tier", tier,
                "isKnownGood", KNOWN_GOOD_COMPANIES.contains(company.toLowerCase().trim()),
                "salaryIndicatesQuality", salaryMin != null && salaryMin > 80000
        );
    }
}
