package com.jobagent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.UUID;

@Entity
@Table(name = "user_profile_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String linkType;

    @Column(nullable = false)
    private String url;

    private String label;

    @CreationTimestamp
    @Column(updatable = false)
    private java.time.OffsetDateTime createdAt;
}
