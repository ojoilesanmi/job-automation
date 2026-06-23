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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GreenhouseConnector implements JobSourceConnector {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getSourceType() {
        return "greenhouse";
    }

    @Override
    public List<Job> fetchJobs(JobSource source, int maxResults) {
        try {
            String boardToken = source.getConfigJson() != null && source.getConfigJson().containsKey("boardToken")
                    ? (String) source.getConfigJson().get("boardToken")
                    : null;

            if (boardToken == null || boardToken.isBlank()) {
                log.warn("Greenhouse: no boardToken configured for source {}", source.getName());
                return Collections.emptyList();
            }

            String url = "https://boards-api.greenhouse.io/v1/boards/" + boardToken + "/jobs";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "JobAgent/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            List<Job> jobs = new ArrayList<>();
            JsonNode jobsNode = root.path("jobs");
            if (jobsNode.isArray()) {
                for (JsonNode node : jobsNode) {
                    if (jobs.size() >= maxResults) break;
                    jobs.add(buildJob(node, source, boardToken));
                }
            }
            log.info("Greenhouse ({}): fetched {} jobs", boardToken, jobs.size());
            return jobs;
        } catch (Exception e) {
            log.error("Greenhouse fetch failed for {}: {}", source.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private Job buildJob(JsonNode node, JobSource source, String boardToken) {
        String jobId = node.path("id").asText();
        String title = node.path("title").asText("");
        String locationName = node.path("location").path("name").asText("");
        String description = node.path("content") != null ? node.path("content").asText("") : "";
        String url = "https://boards.greenhouse.io/" + boardToken + "/jobs/" + jobId;
        String employmentType = node.path("updated_at").isMissingNode() ? "" : mapEmploymentType(node.path("absolute_url").asText());

        return Job.builder()
                .source(source)
                .externalJobId(boardToken + "-" + jobId)
                .title(title)
                .company(source.getName())
                .description(description)
                .applicationUrl(url)
                .location(locationName)
                .country(extractCountry(locationName))
                .remoteType(detectRemoteType(description, locationName))
                .employmentType(employmentType)
                .datePosted(parseDate(node.path("updated_at").asText()))
                .atsProvider("greenhouse")
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

    private String mapEmploymentType(String url) {
        return "";
    }

    private OffsetDateTime parseDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.isBlank()) return null;
            return OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
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
