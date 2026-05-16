package com.project.ieum.repository;

import com.project.ieum.entity.caregiver.CaregiverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaregiverProfileRepository extends JpaRepository<CaregiverProfile, Long> {
}
