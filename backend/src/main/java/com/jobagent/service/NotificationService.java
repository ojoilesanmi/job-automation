package com.jobagent.service;

import com.jobagent.dto.NotificationListResponse;
import com.jobagent.dto.NotificationResponse;
import com.jobagent.model.Notification;
import com.jobagent.model.User;
import com.jobagent.repository.NotificationRepository;
import com.jobagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public void createNotification(UUID userId, String type, String title, String message,
                                    UUID referenceId, String referenceType) {
        User user = new User();
        user.setId(userId);

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .read(false)
                .build();
        notificationRepository.save(notification);

        if (!EMAIL_EXCLUDED_TYPES.contains(type) && emailService.isEmailEnabled()) {
            userRepository.findById(userId).ifPresent(u -> {
                String htmlBody = buildNotificationHtml(title, message, type);
                emailService.sendNotificationEmail(u.getEmail(), title, htmlBody);
            });
        }
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(UUID userId, int page, int size) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        List<NotificationResponse> responses = notifications.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new NotificationListResponse(responses, notifications.getTotalElements(),
                notifications.getTotalPages(), notifications.getNumber());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new com.jobagent.exception.ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new com.jobagent.exception.ForbiddenException("Access denied");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        Page<Notification> unread = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100));
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getReferenceId(), n.getReferenceType(), n.isRead(), n.getCreatedAt()
        );
    }

    private static final Set<String> EMAIL_EXCLUDED_TYPES = Set.of(
            "daily_summary", "match_scored", "weekly_summary"
    );

    private String buildNotificationHtml(String title, String message, String type) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2563eb;">%s</h2>
                    <p>%s</p>
                    <p style="color: #666; font-size: 14px;">Notification type: %s</p>
                </body>
                </html>
                """.formatted(title, message, type);
    }
}
