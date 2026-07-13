package com.jobagent.service;

import com.jobagent.model.Job;
import com.jobagent.model.JobSource;
import com.jobagent.worker.JobSourceConnector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssFeedConnector implements JobSourceConnector {

    private final WebClient.Builder webClientBuilder;

    @Override
    public String getSourceType() {
        return "rss_feed";
    }

    @Override
    public List<Job> fetchJobs(JobSource source, int maxResults) {
        String url = source.getBaseUrl();
        if (url == null || url.isBlank()) {
            log.warn("RSS feed URL not configured for source: {}", source.getName());
            return List.of();
        }

        try {
            String xml = webClientBuilder.build().get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xml == null || xml.isBlank()) {
                log.warn("Empty RSS feed response from {}", url);
                return List.of();
            }

            return parseRssItems(xml, source, maxResults);
        } catch (Exception e) {
            log.error("Failed to fetch RSS feed from {}: {}", url, e.getMessage());
            return List.of();
        }
    }

    public List<Job> fetchJobs(String customUrl, JobSource source, int maxResults) {
        if (customUrl == null || customUrl.isBlank()) {
            return fetchJobs(source, maxResults);
        }

        try {
            String xml = webClientBuilder.build().get()
                    .uri(customUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xml == null || xml.isBlank()) {
                log.warn("Empty RSS feed response from {}", customUrl);
                return List.of();
            }

            return parseRssItems(xml, source, maxResults);
        } catch (Exception e) {
            log.error("Failed to fetch RSS feed from {}: {}", customUrl, e.getMessage());
            return List.of();
        }
    }

    private List<Job> parseRssItems(String xml, JobSource source, int maxResults) {
        List<Job> jobs = new ArrayList<>();
        Pattern itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL);
        Matcher itemMatcher = itemPattern.matcher(xml);

        while (itemMatcher.find() && jobs.size() < maxResults) {
            String item = itemMatcher.group(1);
            String title = extractTag(item, "title");
            String link = extractTag(item, "link");
            String description = extractTag(item, "description");
            String pubDate = extractTag(item, "pubDate");

            if (title == null || title.isBlank()) continue;
            if (link == null || link.isBlank()) continue;

            String company = extractCompany(title);
            OffsetDateTime datePosted = parsePubDate(pubDate);

            Job job = Job.builder()
                    .source(source)
                    .externalJobId(link.trim())
                    .title(title.trim())
                    .company(company)
                    .description(description != null ? cleanHtml(description) : "")
                    .applicationUrl(link.trim())
                    .datePosted(datePosted)
                    .dateDiscovered(OffsetDateTime.now())
                    .build();

            jobs.add(job);
        }

        log.info("Parsed {} job(s) from RSS feed", jobs.size());
        return jobs;
    }

    private String extractTag(String item, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(item);
        if (matcher.find()) {
            String value = matcher.group(1);
            value = value.replaceAll("<[^>]+>", "").trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    private String extractCompany(String title) {
        Pattern atPattern = Pattern.compile("\\s+(?:at|@)\\s+(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = atPattern.matcher(title);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        Pattern dashPattern = Pattern.compile("\\s+-\\s+(.+)");
        matcher = dashPattern.matcher(title);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private OffsetDateTime parsePubDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) return null;

        String[] formats = {
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return OffsetDateTime.parse(pubDate, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        log.debug("Could not parse date: {}", pubDate);
        return null;
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
