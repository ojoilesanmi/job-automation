package com.jobagent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_match_id")
    private JobMatch jobMatch;

    @Column(nullable = false)
    private String feedbackType;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private Double originalScore;

    private String originalStatus;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}
