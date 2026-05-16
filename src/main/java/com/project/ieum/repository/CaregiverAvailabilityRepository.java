package com.project.ieum.repository;

import com.project.ieum.entity.caregiver.CaregiverAvailability;
import com.project.ieum.entity.caregiver.CaregiverAvailabilityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaregiverAvailabilityRepository extends JpaRepository<CaregiverAvailability, CaregiverAvailabilityId> {
}
