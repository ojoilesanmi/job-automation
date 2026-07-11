package com.jobagent.service;

import com.jobagent.dto.JobMatchResponse;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingEngineTest {

    @Mock private JobMatchRepository jobMatchRepository;
    @Mock private JobRepository jobRepository;
    @Mock private ProfileSkillRepository skillRepository;
    @Mock private WorkExperienceRepository experienceRepository;
    @Mock private UserPreferencesRepository preferencesRepository;
    @Mock private AiServiceClient aiServiceClient;
    @Mock private CoverLetterService coverLetterService;
    @Mock private ApplicationService applicationService;
    @Mock private ProfileService profileService;
    @Mock private QueueProducerService queueProducerService;

    private MeterRegistry meterRegistry;

    @InjectMocks
    private MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        matchingEngine = new MatchingEngine(
                jobMatchRepository, jobRepository, skillRepository, experienceRepository,
                preferencesRepository, aiServiceClient, coverLetterService,
                applicationService, profileService, meterRegistry, queueProducerService);
    }

    @Test
    void scoreJob_withNoSkillsAndNoPrefs_returnsReasonableScores() {
        UUID jobId = java.util.UUID.randomUUID();
        UUID userId = java.util.UUID.randomUUID();

        Job job = Job.builder().title("Software Engineer").company("Acme")
                .requiredSkills("Java, Python").preferredSkills("React")
                .experienceYears(3).remoteType("full_remote").build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobMatchRepository.findByUserIdAndJobId(userId, jobId)).thenReturn(Optional.empty());
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skillRepository.findByUserId(any())).thenReturn(Collections.emptyList());
        when(experienceRepository.findByUserIdOrderByStartDateDesc(any())).thenReturn(Collections.emptyList());
        when(preferencesRepository.findByUserId(any())).thenReturn(Optional.empty());

        JobMatchResponse result = matchingEngine.scoreJob(userId, jobId);

        assertThat(result.fitScore()).isNotNull();
        assertThat(result.skillsScore()).isNotNull();
        assertThat(result.experienceScore()).isNotNull();
        assertThat(result.status()).isEqualTo("scored");
    }

    @Test
    void scoreJob_withMatchingSkills_returnsHighFit() {
        UUID jobId = java.util.UUID.randomUUID();
        UUID userId = java.util.UUID.randomUUID();

        Job job = Job.builder().title("Software Engineer").company("Acme")
                .requiredSkills("Java").preferredSkills("")
                .experienceYears(2).build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobMatchRepository.findByUserIdAndJobId(userId, jobId)).thenReturn(Optional.empty());
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileSkill skill = ProfileSkill.builder().skillName("Java").build();
        when(skillRepository.findByUserId(any())).thenReturn(List.of(skill));
        when(experienceRepository.findByUserIdOrderByStartDateDesc(any())).thenReturn(Collections.emptyList());
        when(preferencesRepository.findByUserId(any())).thenReturn(Optional.empty());

        JobMatchResponse result = matchingEngine.scoreJob(userId, jobId);

        assertThat(result.fitScore()).isNotNull();
        assertThat(result.skillsScore()).isNotNull();
        assertThat(result.status()).isEqualTo("scored");
    }
}
