package com.jobagent.worker;

import com.jobagent.model.Job;
import com.jobagent.model.JobSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeverConnector implements JobSourceConnector {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getSourceType() {
        return "lever";
    }

    @Override
    public List<Job> fetchJobs(JobSource source, int maxResults) {
        try {
            String company = source.getConfigJson() != null && source.getConfigJson().containsKey("company")
                    ? (String) source.getConfigJson().get("company")
                    : null;

            if (company == null || company.isBlank()) {
                log.warn("Lever: no company configured for source {}", source.getName());
                return Collections.emptyList();
            }

            String url = "https://api.lever.co/v0/postings/" + company;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "JobAgent/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            List<Job> jobs = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    if (jobs.size() >= maxResults) break;
                    jobs.add(buildJob(node, source, company));
                }
            }
            log.info("Lever ({}): fetched {} jobs", company, jobs.size());
            return jobs;
        } catch (Exception e) {
            log.error("Lever fetch failed for {}: {}", source.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private Job buildJob(JsonNode node, JobSource source, String company) {
        String postId = node.path("id").asText("");
        String text = node.path("text").asText("");
        String description = node.path("descriptionPlain") != null
                ? node.path("descriptionPlain").asText("")
                : node.path("description").asText("");
        String hostedUrl = node.path("hostedUrl").asText("");
        String categoriesTeam = node.path("categories").path("team").asText("");
        String categoriesDepartment = node.path("categories").path("department").asText("");
        String categoriesLocation = node.path("categories").path("location").asText("");
        String commitment = node.path("categories").path("commitment").asText("");

        String title = text.contains(" - ") ? text.split(" - ")[0].trim() : text;
        String companyFromText = text.contains(" - ") ? text.split(" - ")[1].trim() : company;

        return Job.builder()
                .source(source)
                .externalJobId(postId)
                .title(title)
                .company(companyFromText)
                .description(description)
                .applicationUrl(hostedUrl)
                .location(categoriesLocation)
                .country(extractCountry(categoriesLocation))
                .remoteType(detectRemoteType(description, categoriesLocation))
                .seniority(categoriesTeam)
                .employmentType(commitment)
                .datePosted(parseDate(node.path("createdAt").asLong()))
                .rawPayload(toMap(node))
                .build();
    }

    private String extractCountry(String location) {
        if (location == null || location.isBlank()) return "";
        String[] parts = location.split(",");
        return parts[parts.length - 1].trim();
    }

    private String detectRemoteType(String description, String location) {
        String lower = ((description != null ? description : "") + " " + (location != null ? location : "")).toLowerCase();
        if (lower.contains("remote")) return "full_remote";
        if (lower.contains("hybrid")) return "hybrid";
        return "onsite";
    }

    private OffsetDateTime parseDate(long epochMillis) {
        if (epochMillis == 0) return null;
        return OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMillis), java.time.ZoneId.systemDefault());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode node) {
        try {
            return objectMapper.convertValue(node, Map.class);
        } catch (Exception e) {
            return Map.of("raw", node.toString());
        }
    }
}
