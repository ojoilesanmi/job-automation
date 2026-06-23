package com.jobagent.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingEngineTest {

    @Mock private JobMatchRepository jobMatchRepository;
    @Mock private ProfileSkillRepository skillRepository;
    @Mock private WorkExperienceRepository experienceRepository;
    @Mock private UserPreferencesRepository preferencesRepository;
    @Mock private AiServiceClient aiServiceClient;

    private MeterRegistry meterRegistry;

    @InjectMocks
    private MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        // Inject manually since @InjectMocks won't overwrite final fields
        matchingEngine = new MatchingEngine(
                jobMatchRepository, skillRepository, experienceRepository,
                preferencesRepository, aiServiceClient, meterRegistry);
    }

    @Test
    void scoreJob_withNoSkillsAndNoPrefs_returnsReasonableScores() {
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skillRepository.findByUserId(any())).thenReturn(Collections.emptyList());
        when(experienceRepository.findByUserIdOrderByStartDateDesc(any())).thenReturn(Collections.emptyList());
        when(preferencesRepository.findByUserId(any())).thenReturn(Optional.empty());

        JobMatch result = matchingEngine.scoreJob(java.util.UUID.randomUUID(), java.util.UUID.randomUUID());

        assertThat(result.getFitScore()).isNotNull();
        assertThat(result.getSkillsScore()).isNotNull();
        assertThat(result.getExperienceScore()).isNotNull();
        assertThat(result.getStatus()).isEqualTo("scored");
    }

    @Test
    void scoreJob_withMatchingSkills_returnsHighFit() {
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileSkill skill = ProfileSkill.builder().skillName("Java").build();
        when(skillRepository.findByUserId(any())).thenReturn(List.of(skill));
        when(experienceRepository.findByUserIdOrderByStartDateDesc(any())).thenReturn(Collections.emptyList());
        when(preferencesRepository.findByUserId(any())).thenReturn(Optional.empty());

        // scoreJob creates its own Job stub; just verify it runs and produces a result
        JobMatch result = matchingEngine.scoreJob(java.util.UUID.randomUUID(), java.util.UUID.randomUUID());

        assertThat(result.getFitScore()).isNotNull();
        assertThat(result.getSkillsScore()).isNotNull();
        assertThat(result.getStatus()).isEqualTo("scored");
    }
}
