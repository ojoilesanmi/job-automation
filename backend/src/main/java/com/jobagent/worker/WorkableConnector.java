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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkableConnector implements JobSourceConnector {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getSourceType() {
        return "workable";
    }

    @Override
    public List<Job> fetchJobs(JobSource source, int maxResults) {
        try {
            String baseUrl = source.getBaseUrl();
            if (baseUrl == null || baseUrl.isBlank()) {
                log.warn("Workable: no baseUrl configured for source {}", source.getName());
                return Collections.emptyList();
            }

            String apiKey = source.getConfigJson() != null
                    ? (String) source.getConfigJson().get("apiKey")
                    : null;
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("Workable: no apiKey configured for source {}", source.getName());
                return Collections.emptyList();
            }

            String url = baseUrl.endsWith("/") ? baseUrl + "jobs" : baseUrl + "/jobs";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", "JobAgent/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            List<Job> jobs = new ArrayList<>();
            JsonNode jobsNode = root.has("jobs") ? root.get("jobs") : root;
            if (jobsNode.isArray()) {
                for (JsonNode node : jobsNode) {
                    if (jobs.size() >= maxResults) break;
                    jobs.add(buildJob(node, source));
                }
            }
            log.info("Workable: fetched {} jobs from {}", jobs.size(), source.getName());
            return jobs;
        } catch (Exception e) {
            log.error("Workable fetch failed for {}: {}", source.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private Job buildJob(JsonNode node, JobSource source) {
        String title = node.path("title").asText("");
        String department = node.path("department").asText("");
        String location = node.path("city").asText("") + ", " + node.path("country").asText("");
        String url = node.path("url").asText("");
        String description = node.path("description").asText("");

        return Job.builder()
                .source(source)
                .externalJobId(node.path("id").asText(""))
                .title(title)
                .company(source.getName())
                .description(description)
                .applicationUrl(url)
                .location(location.trim())
                .country(node.path("country").asText(""))
                .remoteType("full_remote")
                .employmentType(node.path("employment_type").asText(""))
                .rawPayload(toMap(node))
                .build();
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
