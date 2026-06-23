package com.jobagent.service;

import com.jobagent.dto.ImportJobRequest;
import com.jobagent.dto.JobListResponse;
import com.jobagent.dto.JobResponse;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.Job;
import com.jobagent.repository.JobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public JobResponse importJob(ImportJobRequest request) {
        Job job = Job.builder()
                .title(request.title())
                .company(request.company() != null ? request.company() : "Unknown")
                .description(request.description() != null ? request.description() : "")
                .location(request.location())
                .country(request.country())
                .salaryMin(request.salaryMin())
                .salaryMax(request.salaryMax())
                .currency(request.currency())
                .remoteType(request.remoteType())
                .relocationAvailable(request.relocationAvailable() != null ? request.relocationAvailable() : false)
                .seniority(request.seniority())
                .requiredSkills(request.requiredSkills())
                .applicationUrl(request.applicationUrl() != null ? request.applicationUrl() : request.url())
                .build();

        job = jobRepository.save(job);
        Counter.builder("jobagent.jobs.imported")
                .description("Number of jobs imported")
                .tag("source", "manual")
                .register(meterRegistry)
                .increment();
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public JobListResponse searchJobs(String company, String country, String remoteType,
                                       String search, int page, int size) {
        Page<Job> jobs = jobRepository.searchJobs(
                company, country, remoteType, search,
                PageRequest.of(page, size)
        );

        List<JobResponse> jobResponses = jobs.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new JobListResponse(jobResponses, jobs.getTotalElements(), jobs.getTotalPages(), jobs.getNumber());
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        return toResponse(job);
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(), job.getExternalJobId(),
                job.getSource() != null ? job.getSource().getName() : null,
                job.getTitle(), job.getCompany(), job.getCompanyWebsite(),
                job.getDescription(), job.getLocation(), job.getCountry(),
                job.getSalaryMin(), job.getSalaryMax(), job.getCurrency(),
                job.getRemoteType(), job.getRelocationAvailable(), job.getVisaSponsorshipSignal(),
                job.getSeniority(), job.getRequiredSkills(), job.getPreferredSkills(),
                job.getExperienceYears(), job.getEmploymentType(),
                job.getApplicationUrl(), job.getAtsProvider(),
                job.getDatePosted(), job.getDateDiscovered()
        );
    }
}
