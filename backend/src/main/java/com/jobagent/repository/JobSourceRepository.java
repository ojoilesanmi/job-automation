package com.jobagent.repository;

import com.jobagent.model.JobSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobSourceRepository extends JpaRepository<JobSource, UUID> {
    List<JobSource> findByEnabledTrue();
}
