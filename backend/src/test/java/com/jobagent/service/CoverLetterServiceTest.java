package com.jobagent.service;

import com.jobagent.dto.CoverLetterResponse;
import com.jobagent.dto.GenerateCoverLetterRequest;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoverLetterServiceTest {

    @Mock private CoverLetterRepository coverLetterRepository;
    @Mock private JobRepository jobRepository;
    @Mock private CvDocumentRepository cvDocumentRepository;
    @Mock private ProfileSkillRepository skillRepository;
    @Mock private WorkExperienceRepository experienceRepository;
    @Mock private UserProfileRepository profileRepository;
    @Mock private AiServiceClient aiServiceClient;

    private MeterRegistry meterRegistry;
    private CoverLetterService coverLetterService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        coverLetterService = new CoverLetterService(
                coverLetterRepository, jobRepository, cvDocumentRepository,
                skillRepository, experienceRepository, profileRepository,
                aiServiceClient, meterRegistry);
    }

    @Test
    void generateCoverLetter_whenJobNotFound_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        GenerateCoverLetterRequest request = new GenerateCoverLetterRequest(jobId, null, null);

        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coverLetterService.generateCoverLetter(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Job not found");
    }

    @Test
    void generateCoverLetter_withAiService_usesAiContent() throws Exception {
        UUID userId = UUID.randomUUID();
        Job job = Job.builder()
                .id(UUID.randomUUID())
                .title("Software Engineer")
                .company("TestCo")
                .description("Build great things")
                .build();

        GenerateCoverLetterRequest request = new GenerateCoverLetterRequest(job.getId(), null, "professional");

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(cvDocumentRepository.findByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.empty());
        when(skillRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        when(experienceRepository.findByUserIdOrderByStartDateDesc(userId)).thenReturn(Collections.emptyList());
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(coverLetterRepository.save(any(CoverLetter.class))).thenAnswer(inv -> inv.getArgument(0));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode aiResult = mapper.readTree("{\"content\": \"Dear TestCo team, I am excited...\"}");
        when(aiServiceClient.generateCoverLetter(any())).thenReturn(aiResult);

        CoverLetterResponse response = coverLetterService.generateCoverLetter(userId, request);

        assertThat(response.content()).isEqualTo("Dear TestCo team, I am excited...");
        assertThat(response.status()).isEqualTo("draft");
    }

    @Test
    void generateCoverLetter_withNoAi_fallsBackToTemplate() {
        UUID userId = UUID.randomUUID();
        Job job = Job.builder()
                .id(UUID.randomUUID())
                .title("Developer")
                .company("Acme")
                .description("Java spring microservices")
                .build();

        GenerateCoverLetterRequest request = new GenerateCoverLetterRequest(job.getId(), null, null);

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(cvDocumentRepository.findByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.empty());
        when(skillRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        when(experienceRepository.findByUserIdOrderByStartDateDesc(userId)).thenReturn(Collections.emptyList());
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(coverLetterRepository.save(any(CoverLetter.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiServiceClient.generateCoverLetter(any())).thenReturn(null);

        CoverLetterResponse response = coverLetterService.generateCoverLetter(userId, request);

        assertThat(response.content()).contains("Dear Hiring Manager");
        assertThat(response.content()).contains("Acme");
    }
}
