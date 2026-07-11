package com.jobagent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "education")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String institution;

    private String degree;

    private String fieldOfStudy;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private java.time.OffsetDateTime createdAt;
}
