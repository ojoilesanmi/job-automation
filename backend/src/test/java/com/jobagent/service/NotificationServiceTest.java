package com.jobagent.service;

import com.jobagent.dto.NotificationListResponse;
import com.jobagent.dto.NotificationResponse;
import com.jobagent.exception.ForbiddenException;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.Notification;
import com.jobagent.model.User;
import com.jobagent.repository.NotificationRepository;
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
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private UUID notificationId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        notification = Notification.builder()
                .id(notificationId)
                .user(user)
                .type("follow_up_due")
                .title("Follow-up Due")
                .message("Time to follow up on your application")
                .read(false)
                .build();
    }

    @Test
    void createNotification_savesSuccessfully() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.createNotification(userId, "follow_up_due", "Follow-up Due",
                "Time to follow up", null, null);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void getNotifications_returnsList() {
        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10)))
                .thenReturn(page);

        NotificationListResponse response = notificationService.getNotifications(userId, 0, 10);

        assertThat(response.notifications()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(5L);

        long count = notificationService.getUnreadCount(userId);

        assertThat(count).isEqualTo(5);
    }

    @Test
    void markAsRead_setsReadTrue() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead(userId, notificationId);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markAsRead_notOwner_throwsException() {
        UUID otherUserId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(otherUserId, notificationId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void markAsRead_notFound_throwsException() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(userId, notificationId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllAsRead_updatesAllNotifications() {
        Notification unreadNotification = Notification.builder()
                .id(UUID.randomUUID())
                .user(new User())
                .type("strong_match")
                .title("Strong Match")
                .message("A strong match was found")
                .read(false)
                .build();

        Page<Notification> page = new PageImpl<>(List.of(notification, unreadNotification), PageRequest.of(0, 100), 2);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100)))
                .thenReturn(page);
        when(notificationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAllAsRead(userId);

        assertThat(notification.isRead()).isTrue();
        assertThat(unreadNotification.isRead()).isTrue();
    }
}