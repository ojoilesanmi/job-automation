package com.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.dto.CompanyResearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyResearchService {

    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${app.company-research.cache-ttl-hours:24}")
    private long cacheTtlHours;

    public CompanyResearchResponse researchCompany(String companyName, String description) {
        if (companyName == null || companyName.isBlank()) {
            return new CompanyResearchResponse(companyName, "Company name is required",
                    "Unknown", "Unknown", "Unknown", "N/A", "N/A");
        }

        String cacheKey = "company_research:" + companyName.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (redisTemplate != null) {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, CompanyResearchResponse.class);
                } catch (Exception e) {
                    log.debug("Failed to deserialize cached company research for {}", companyName);
                }
            }
        }

        CompanyResearchResponse result = performResearch(companyName, description);

        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(cacheKey, json, cacheTtlHours, TimeUnit.HOURS);
            } catch (Exception e) {
                log.debug("Failed to cache company research for {}", companyName);
            }
        }

        return result;
    }

    private CompanyResearchResponse performResearch(String companyName, String description) {
        try {
            JsonNode aiResult = aiServiceClient.parseJob(companyName, "", description != null ? description : "");

            if (aiResult != null) {
                return new CompanyResearchResponse(
                        companyName,
                        aiResult.path("companySummary").asText(
                                aiResult.path("summary").asText("No summary available")),
                        aiResult.path("companySize").asText("Unknown"),
                        aiResult.path("industry").asText("Unknown"),
                        aiResult.path("techStack").asText("Unknown"),
                        aiResult.path("glassdoorRating").asText("N/A"),
                        aiResult.path("linkedInUrl").asText("N/A")
                );
            }
        } catch (Exception e) {
            log.warn("AI company research failed for {}: {}", companyName, e.getMessage());
        }

        return defaultResponse(companyName);
    }

    private CompanyResearchResponse defaultResponse(String companyName) {
        return new CompanyResearchResponse(
                companyName,
                "Research data not available for " + companyName,
                "Unknown",
                "Unknown",
                "Unknown",
                "N/A",
                "https://linkedin.com/company/" + companyName.toLowerCase().replaceAll("[^a-z0-9]", "")
        );
    }
}
