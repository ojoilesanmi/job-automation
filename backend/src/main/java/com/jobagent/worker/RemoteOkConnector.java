package com.jobagent.worker;

import com.jobagent.model.Job;
import com.jobagent.model.JobSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteOkConnector implements JobSourceConnector {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getSourceType() {
        return "remoteok";
    }

    @Override
    public List<Job> fetchJobs(JobSource source, int maxResults) {
        try {
            String tag = source.getConfigJson() != null && source.getConfigJson().containsKey("tag")
                    ? (String) source.getConfigJson().get("tag")
                    : "";

            String url = "https://remoteok.com/api" + (tag.isEmpty() ? "" : "/" + tag);
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
                    if (node.has("id") && jobs.size() < maxResults) {
                        jobs.add(buildJob(node, source));
                    }
                }
            }
            log.info("RemoteOK: fetched {} jobs", jobs.size());
            return jobs;
        } catch (Exception e) {
            log.error("RemoteOK fetch failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Job buildJob(JsonNode node, JobSource source) {
        String position = node.path("position").asText("");
        String company = node.path("company").asText("");
        String description = node.path("description").asText("");
        String url = node.path("url").asText("");
        String location = node.path("location").asText("Remote");
        String[] tags = objectMapper.convertValue(node.path("tags"), String[].class);

        return Job.builder()
                .source(source)
                .externalJobId(String.valueOf(node.path("id").asLong()))
                .title(position)
                .company(company)
                .description(description)
                .applicationUrl(url)
                .location(location)
                .country(extractCountry(location))
                .remoteType("full_remote")
                .requiredSkills(tags != null ? String.join(", ", tags) : "")
                .salaryMin(node.has("salary_min") && !node.get("salary_min").isNull()
                        ? BigDecimal.valueOf(node.path("salary_min").asDouble()) : null)
                .salaryMax(node.has("salary_max") && !node.get("salary_max").isNull()
                        ? BigDecimal.valueOf(node.path("salary_max").asDouble()) : null)
                .currency("USD")
                .datePosted(parseDate(node.path("epoch").asLong()))
                .rawPayload(toMap(node))
                .build();
    }

    private String extractCountry(String location) {
        if (location == null || location.isBlank()) return "Remote";
        String[] parts = location.split(",");
        return parts[parts.length - 1].trim();
    }

    private OffsetDateTime parseDate(long epoch) {
        if (epoch == 0) return null;
        return Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).toOffsetDateTime();
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
