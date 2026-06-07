package com.project.ieum.repository;

import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.caregiver.CaregiverServiceRegion;
import com.project.ieum.entity.caregiver.CaregiverServiceRegionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaregiverServiceRegionRepository extends JpaRepository<CaregiverServiceRegion, CaregiverServiceRegionId> {
    List<CaregiverServiceRegion> findByCaregiver(CaregiverProfile caregiver);
    void deleteByCaregiver(CaregiverProfile caregiver);
}
