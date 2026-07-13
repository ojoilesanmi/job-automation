package com.jobagent.repository;

import com.jobagent.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findBySourceIdAndExternalJobId(UUID sourceId, String externalJobId);

    boolean existsBySourceIdAndExternalJobId(UUID sourceId, String externalJobId);

    Optional<Job> findByApplicationUrl(String applicationUrl);

    boolean existsByTitleAndCompany(String title, String company);

    List<Job> findByTitleContainingIgnoreCaseAndCompanyContainingIgnoreCase(String titleKeyword, String company);

    @Query(value = "SELECT j.* FROM jobs j " +
           "LEFT JOIN job_sources js ON j.source_id = js.id " +
           "LEFT JOIN job_matches jm ON j.id = jm.job_id AND jm.user_id = :userId " +
           "WHERE " +
           "(:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :company, '%'))) " +
           "AND (:country IS NULL OR LOWER(j.country) = LOWER(:country)) " +
           "AND (:remoteType IS NULL OR j.remote_type = :remoteType) " +
           "AND (:source IS NULL OR js.name = :source) " +
           "AND (:relocation IS NULL OR j.relocation_available = :relocation) " +
           "AND (:salaryMin IS NULL OR j.salary_max >= :salaryMin) " +
           "AND (:seniority IS NULL OR j.seniority = :seniority) " +
           "AND (:role IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :role, '%'))) " +
           "AND (:datePostedAfter IS NULL OR j.date_posted >= :datePostedAfter) " +
           "AND (:fitScoreMin IS NULL OR jm.fit_score >= :fitScoreMin) " +
           "AND (:search IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(j.company) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY j.date_discovered DESC",
           countQuery = "SELECT COUNT(*) FROM jobs j " +
           "LEFT JOIN job_sources js ON j.source_id = js.id " +
           "LEFT JOIN job_matches jm ON j.id = jm.job_id AND jm.user_id = :userId " +
           "WHERE " +
           "(:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :company, '%'))) " +
           "AND (:country IS NULL OR LOWER(j.country) = LOWER(:country)) " +
           "AND (:remoteType IS NULL OR j.remote_type = :remoteType) " +
           "AND (:source IS NULL OR js.name = :source) " +
           "AND (:relocation IS NULL OR j.relocation_available = :relocation) " +
           "AND (:salaryMin IS NULL OR j.salary_max >= :salaryMin) " +
           "AND (:seniority IS NULL OR j.seniority = :seniority) " +
           "AND (:role IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :role, '%'))) " +
           "AND (:datePostedAfter IS NULL OR j.date_posted >= :datePostedAfter) " +
           "AND (:fitScoreMin IS NULL OR jm.fit_score >= :fitScoreMin) " +
           "AND (:search IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(j.company) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<Job> searchJobs(
            @Param("userId") UUID userId,
            @Param("company") String company,
            @Param("country") String country,
            @Param("remoteType") String remoteType,
            @Param("source") String source,
            @Param("relocation") Boolean relocation,
            @Param("salaryMin") java.math.BigDecimal salaryMin,
            @Param("seniority") String seniority,
            @Param("role") String role,
            @Param("datePostedAfter") java.time.OffsetDateTime datePostedAfter,
            @Param("fitScoreMin") java.math.BigDecimal fitScoreMin,
            @Param("search") String search,
            Pageable pageable);
}
