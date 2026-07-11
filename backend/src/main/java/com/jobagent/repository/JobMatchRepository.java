package com.jobagent.repository;

import com.jobagent.model.JobMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {

    Page<JobMatch> findByUserIdOrderByFitScoreDesc(UUID userId, Pageable pageable);

    Page<JobMatch> findByUserIdAndStatusOrderByFitScoreDesc(UUID userId, String status, Pageable pageable);

    Optional<JobMatch> findByUserIdAndJobId(UUID userId, UUID jobId);

    long countByUserIdAndStatus(UUID userId, String status);

    long countByUserId(UUID userId);

    @Query(value = "SELECT jm.status, COUNT(*) FROM job_matches jm WHERE jm.user_id = :userId GROUP BY jm.status", nativeQuery = true)
    List<Object[]> countByStatusGrouped(@Param("userId") UUID userId);

    @Query("SELECT COUNT(jm) FROM JobMatch jm WHERE jm.user.id = :userId AND jm.createdAt BETWEEN :start AND :end")
    long countByUserIdAndDateRange(@Param("userId") UUID userId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    List<JobMatch> findByUserIdAndStatusIn(UUID userId, List<String> statuses);

    @Query("SELECT COALESCE(AVG(jm.fitScore), 0) FROM JobMatch jm WHERE jm.user.id = :userId")
    double averageFitScoreByUserId(@Param("userId") UUID userId);
}
