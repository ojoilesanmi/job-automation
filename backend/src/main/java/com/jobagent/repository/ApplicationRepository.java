package com.jobagent.repository;

import com.jobagent.model.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Page<Application> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Application> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status, Pageable pageable);

    Optional<Application> findByUserIdAndJobId(UUID userId, UUID jobId);

    long countByUserIdAndStatus(UUID userId, String status);

    @Query(value = "SELECT COUNT(*) FROM applications a WHERE a.user_id = :userId " +
           "AND a.submitted_at >= CURRENT_DATE AND a.submitted_at < CURRENT_DATE + INTERVAL '1 day'", nativeQuery = true)
    long countTodaySubmissions(@Param("userId") UUID userId);
}
