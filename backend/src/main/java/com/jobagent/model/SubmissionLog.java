package com.jobagent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "submission_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private String screenshotPath;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}
