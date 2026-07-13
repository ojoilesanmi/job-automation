package com.jobagent.service;

import com.jobagent.dto.ImportJobRequest;
import com.jobagent.model.Job;
import com.jobagent.model.JobSource;
import com.jobagent.repository.JobRepository;
import com.jobagent.repository.JobSourceRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.InetAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobImportService {

    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;
    private final MeterRegistry meterRegistry;
    private final DuplicateJobDetectionService duplicateJobDetectionService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Transactional
    public Job importFromUrl(ImportJobRequest request) {
        String description = fetchAndExtractDescription(request.url());

        JobSource manualSource = jobSourceRepository.findBySourceType("manual").stream().findFirst()
                .orElseGet(() -> createManualSource());

        Job job = Job.builder()
                .source(manualSource)
                .externalJobId(request.url().hashCode() + "")
                .title(request.title() != null ? request.title() : "Imported Job")
                .company(request.company() != null ? request.company() : "Unknown")
                .description(description != null ? description : "")
                .applicationUrl(request.url())
                .dateDiscovered(OffsetDateTime.now())
                .rawPayload(Map.of("importedUrl", request.url(), "importedAt", OffsetDateTime.now().toString()))
                .build();

        Optional<Job> existing = duplicateJobDetectionService.findDuplicate(job);
        if (existing.isPresent()) {
            Counter.builder("jobagent.jobs.duplicates_detected")
                    .description("Number of duplicate jobs detected")
                    .tag("source", "manual")
                    .register(meterRegistry)
                    .increment();
            return existing.get();
        }

        return jobRepository.save(job);
    }

    private String fetchAndExtractDescription(String url) {
        try {
            URI uri = validateImportUrl(url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "JobAgent/1.0 (job-import)")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalArgumentException("Unexpected HTTP status: " + response.statusCode());
            }
            String html = response.body();
            return extractTextFromHtml(html);
        } catch (Exception e) {
            log.warn("Failed to fetch job URL {}: {}", url, e.getMessage());
            return null;
        }
    }

    private URI validateImportUrl(String url) throws Exception {
        URI uri = URI.create(url);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only http/https URLs are allowed");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL host is required");
        }
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("Private/internal URLs are not allowed");
            }
        }
        return uri;
    }

    private String extractTextFromHtml(String html) {
        String text = html.replaceAll("<script[^>]*>[\\s\\S]*?</script>", "")
                .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return text.length() > 5000 ? text.substring(0, 5000) : text;
    }

    private JobSource createManualSource() {
        JobSource source = JobSource.builder()
                .name("Manual Import")
                .sourceType("manual")
                .enabled(true)
                .build();
        return jobSourceRepository.save(source);
    }
}
