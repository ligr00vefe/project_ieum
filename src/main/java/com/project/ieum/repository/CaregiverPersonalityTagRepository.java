package com.project.ieum.repository;

import com.project.ieum.entity.caregiver.CaregiverPersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverPersonalityTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaregiverPersonalityTagRepository extends JpaRepository<CaregiverPersonalityTag, CaregiverPersonalityTagId> {
}
