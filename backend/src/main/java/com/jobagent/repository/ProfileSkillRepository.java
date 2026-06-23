package com.jobagent.repository;

import com.jobagent.model.ProfileSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileSkillRepository extends JpaRepository<ProfileSkill, UUID> {
    List<ProfileSkill> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
