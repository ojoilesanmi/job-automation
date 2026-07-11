package com.jobagent.repository;

import com.jobagent.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findBySourceIdAndExternalJobId(UUID sourceId, String externalJobId);

    boolean existsBySourceIdAndExternalJobId(UUID sourceId, String externalJobId);

    Optional<Job> findByApplicationUrl(String applicationUrl);

    @Query(value = "SELECT j.* FROM jobs j WHERE " +
           "(:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :company, '%'))) " +
           "AND (:country IS NULL OR LOWER(j.country) = LOWER(:country)) " +
           "AND (:remoteType IS NULL OR j.remote_type = :remoteType) " +
           "AND (:search IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(j.company) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY j.date_discovered DESC",
           countQuery = "SELECT COUNT(*) FROM jobs j WHERE " +
           "(:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :company, '%'))) " +
           "AND (:country IS NULL OR LOWER(j.country) = LOWER(:country)) " +
           "AND (:remoteType IS NULL OR j.remote_type = :remoteType) " +
           "AND (:search IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(j.company) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<Job> searchJobs(
            @Param("company") String company,
            @Param("country") String country,
            @Param("remoteType") String remoteType,
            @Param("search") String search,
            Pageable pageable);
}
