package com.project.ieum.repository.search;

import com.project.ieum.dto.search.CaregiverSearchCondition;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CaregiverSearchRepository {
    Page<CaregiverProfile> searchCaregivers(CaregiverSearchCondition condition, Pageable pageable);
}
