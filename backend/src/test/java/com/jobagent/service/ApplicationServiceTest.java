package com.jobagent.service;

import com.jobagent.dto.*;
import com.jobagent.exception.ForbiddenException;
import com.jobagent.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationEventRepository eventRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private CvDocumentRepository cvDocumentRepository;
    @Mock
    private CoverLetterRepository coverLetterRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private MeterRegistry meterRegistry;

    @InjectMocks
    private ApplicationService applicationService;

    private UUID userId;
    private UUID jobId;
    private Job job;
    private Application application;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        applicationService = new ApplicationService(
                applicationRepository, eventRepository, jobRepository,
                cvDocumentRepository, coverLetterRepository, auditLogRepository, meterRegistry
        );
        applicationService.init("discovered,shortlisted,pending_approval,approved,submitted,viewed,interview,assessment,offer,accepted,rejected,no_response,follow_up_needed,withdrawn");

        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        job = Job.builder()
                .id(jobId)
                .title("Software Engineer")
                .company("Acme Corp")
                .description("Test job")
                .build();

        application = Application.builder()
                .id(UUID.randomUUID())
                .user(user)
                .job(job)
                .status("pending_approval")
                .build();
    }

    @Test
    void createApplication_createsSuccessfully() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.findByUserIdAndJobId(userId, jobId)).thenReturn(Optional.empty());
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateApplicationRequest request = new CreateApplicationRequest(jobId, null, null, "approval");

        ApplicationResponse response = applicationService.createApplication(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("pending_approval");
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void createApplication_duplicate_throwsException() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.findByUserIdAndJobId(userId, jobId)).thenReturn(Optional.of(application));

        CreateApplicationRequest request = new CreateApplicationRequest(jobId, null, null, "approval");

        assertThatThrownBy(() -> applicationService.createApplication(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application already exists for this job");
    }

    @Test
    void updateStatus_validTransition_updatesSuccessfully() {
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = applicationService.updateStatus(userId, application.getId(), "shortlisted");

        assertThat(response.status()).isEqualTo("shortlisted");
        verify(eventRepository, times(1)).save(any(ApplicationEvent.class));
    }

    @Test
    void updateStatus_viewed_updatesSuccessfully() {
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = applicationService.updateStatus(userId, application.getId(), "viewed");

        assertThat(response.status()).isEqualTo("viewed");
    }

    @Test
    void updateStatus_noResponse_updatesSuccessfully() {
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = applicationService.updateStatus(userId, application.getId(), "no_response");

        assertThat(response.status()).isEqualTo("no_response");
    }

    @Test
    void updateStatus_followUpNeeded_updatesSuccessfully() {
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = applicationService.updateStatus(userId, application.getId(), "follow_up_needed");

        assertThat(response.status()).isEqualTo("follow_up_needed");
    }

    @Test
    void updateStatus_invalidTransition_throwsException() {
        assertThatThrownBy(() -> applicationService.updateStatus(userId, application.getId(), "invalid_status"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid application status");
    }

    @Test
    void updateStatus_notOwner_throwsException() {
        UUID otherUserId = UUID.randomUUID();
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.updateStatus(otherUserId, application.getId(), "shortlisted"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void submitApplication_setsSubmittedAt() {
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = applicationService.submitApplication(userId, application.getId());

        assertThat(response.status()).isEqualTo("submitted");
    }

    @Test
    void getApplications_filtersByStatus() {
        Page<Application> page = new PageImpl<>(List.of(application), PageRequest.of(0, 10), 1);
        when(applicationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "pending_approval", PageRequest.of(0, 10)))
                .thenReturn(page);

        ApplicationListResponse response = applicationService.getApplications(userId, "pending_approval", 0, 10);

        assertThat(response.applications()).hasSize(1);
    }

    @Test
    void addNote_updatesNotes() {
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = applicationService.addNote(userId, application.getId(), "Follow up next week");

        assertThat(response.notes()).isEqualTo("Follow up next week");
    }
}
