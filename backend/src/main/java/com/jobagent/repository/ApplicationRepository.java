package com.jobagent.repository;

import com.jobagent.model.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
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

    List<Application> findByUserIdAndNextFollowUpAtIsNotNullOrderByNextFollowUpAtAsc(UUID userId, Pageable pageable);

    List<Application> findByNextFollowUpAtBefore(OffsetDateTime dateTime);

    long countByUserId(UUID userId);

    long countByUserIdAndSubmittedAtAfter(UUID userId, OffsetDateTime since);

    @Query(value = "SELECT a.status, COUNT(*) FROM applications a WHERE a.user_id = :userId GROUP BY a.status", nativeQuery = true)
    List<Object[]> countByStatusGrouped(@Param("userId") UUID userId);

    @Query(value = "SELECT j.country, COUNT(*) FROM applications a JOIN jobs j ON a.job_id = j.id WHERE a.user_id = :userId AND j.country IS NOT NULL GROUP BY j.country", nativeQuery = true)
    List<Object[]> countByCountryGrouped(@Param("userId") UUID userId);

    @Query(value = "SELECT j.source_id, js.name, COUNT(*) FROM applications a JOIN jobs j ON a.job_id = j.id JOIN job_sources js ON j.source_id = js.id WHERE a.user_id = :userId GROUP BY j.source_id, js.name", nativeQuery = true)
    List<Object[]> countBySourceGrouped(@Param("userId") UUID userId);
}
