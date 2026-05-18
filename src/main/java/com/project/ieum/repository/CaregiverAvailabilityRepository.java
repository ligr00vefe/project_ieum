package com.project.ieum.repository;

import com.project.ieum.entity.caregiver.CaregiverAvailability;
import com.project.ieum.entity.caregiver.CaregiverAvailabilityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaregiverAvailabilityRepository extends JpaRepository<CaregiverAvailability, CaregiverAvailabilityId> {
    List<CaregiverAvailability> findByCaregiver_UserId(Long caregiverId);
}
