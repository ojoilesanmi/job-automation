package com.jobagent.service;

import com.jobagent.model.Job;
import com.jobagent.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DuplicateJobDetectionService {

    private final JobRepository jobRepository;

    public Optional<Job> findDuplicate(Job job) {
        if (job.getApplicationUrl() != null && !job.getApplicationUrl().isBlank()) {
            Optional<Job> byUrl = jobRepository.findByApplicationUrl(job.getApplicationUrl());
            if (byUrl.isPresent()) return byUrl;
            String normalized = normalizeUrl(job.getApplicationUrl());
            if (!normalized.equals(job.getApplicationUrl())) {
                byUrl = jobRepository.findByApplicationUrl(normalized);
                if (byUrl.isPresent()) return byUrl;
            }
        }

        if (job.getSource() != null && job.getSource().getId() != null && job.getExternalJobId() != null
                && jobRepository.existsBySourceIdAndExternalJobId(job.getSource().getId(), job.getExternalJobId())) {
            return jobRepository.findBySourceIdAndExternalJobId(job.getSource().getId(), job.getExternalJobId());
        }

        if (job.getTitle() != null && job.getCompany() != null) {
            if (jobRepository.existsByTitleAndCompany(job.getTitle(), job.getCompany())) {
                return jobRepository.findByTitleContainingIgnoreCaseAndCompanyContainingIgnoreCase(job.getTitle(), job.getCompany())
                        .stream().findFirst();
            }
            String keyword = extractKeyWords(job.getTitle());
            if (!keyword.isBlank()) {
                return jobRepository.findByTitleContainingIgnoreCaseAndCompanyContainingIgnoreCase(keyword, job.getCompany())
                        .stream().findFirst();
            }
        }
        return Optional.empty();
    }

    public String normalizeUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "https";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            String path = uri.getPath() != null ? uri.getPath().replaceAll("/+$", "") : "";
            return URI.create(scheme + "://" + host + path).toString();
        } catch (Exception ignored) {
            return url;
        }
    }

    private String extractKeyWords(String title) {
        String[] stopWords = {"senior", "junior", "staff", "principal", "lead", "i", "ii", "iii", "iv",
                "software", "engineer", "developer", "manager", "analyst", "the", "a", "an"};
        return Arrays.stream(title.toLowerCase().split("\\s+"))
                .filter(w -> w.length() > 2 && Arrays.stream(stopWords).noneMatch(s -> s.equals(w)))
                .limit(3)
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }
}
