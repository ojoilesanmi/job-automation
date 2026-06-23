package com.jobagent.repository;

import com.jobagent.model.ApplicationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, UUID> {
    List<ApplicationEvent> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
}
