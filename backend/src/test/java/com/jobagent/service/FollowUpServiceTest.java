package com.jobagent.service;

import com.jobagent.dto.FollowUpResponse;
import com.jobagent.exception.ForbiddenException;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.Application;
import com.jobagent.model.Job;
import com.jobagent.model.User;
import com.jobagent.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FollowUpService followUpService;

    private UUID userId;
    private UUID applicationId;
    private Application application;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        applicationId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Job job = Job.builder()
                .id(UUID.randomUUID())
                .title("Software Engineer")
                .company("Acme Corp")
                .build();

        application = Application.builder()
                .id(applicationId)
                .user(user)
                .job(job)
                .status("submitted")
                .build();
    }

    @Test
    void scheduleFollowUp_setsNextFollowUpAt() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        OffsetDateTime followUpDate = OffsetDateTime.now().plusDays(7);
        FollowUpResponse response = followUpService.scheduleFollowUp(userId, applicationId, followUpDate);

        assertThat(response.applicationId()).isEqualTo(applicationId);
        assertThat(response.nextFollowUpAt()).isEqualTo(followUpDate);
    }

    @Test
    void scheduleFollowUp_notOwner_throwsException() {
        UUID otherUserId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        OffsetDateTime followUpDate = OffsetDateTime.now().plusDays(7);
        assertThatThrownBy(() -> followUpService.scheduleFollowUp(otherUserId, applicationId, followUpDate))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void scheduleFollowUp_applicationNotFound_throwsException() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        OffsetDateTime followUpDate = OffsetDateTime.now().plusDays(7);
        assertThatThrownBy(() -> followUpService.scheduleFollowUp(userId, applicationId, followUpDate))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUpcomingFollowUps_returnsList() {
        Application appWithFollowUp = Application.builder()
                .id(UUID.randomUUID())
                .user(new User())
                .job(Job.builder().title("Developer").company("Tech Co").build())
                .nextFollowUpAt(OffsetDateTime.now().plusDays(3))
                .build();

        when(applicationRepository.findByUserIdAndNextFollowUpAtIsNotNullOrderByNextFollowUpAtAsc(
                any(UUID.class), any())).thenReturn(List.of(appWithFollowUp));

        List<FollowUpResponse> responses = followUpService.getUpcomingFollowUps(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).nextFollowUpAt()).isNotNull();
    }

    @Test
    void checkFollowUps_processesDueApplications() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Application dueApp = Application.builder()
                .id(UUID.randomUUID())
                .user(user)
                .job(Job.builder().title("Developer").company("Tech Co").build())
                .nextFollowUpAt(OffsetDateTime.now().minusDays(1))
                .build();

        when(applicationRepository.findByNextFollowUpAtBefore(any(OffsetDateTime.class)))
                .thenReturn(List.of(dueApp));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        followUpService.checkFollowUps();

        verify(notificationService, times(1)).createNotification(
                any(UUID.class), eq("follow_up_due"), eq("Follow-up due"),
                anyString(), any(UUID.class), eq("application")
        );
        assertThat(dueApp.getLastFollowUpAt()).isNotNull();
        assertThat(dueApp.getNextFollowUpAt()).isNull();
    }
}
