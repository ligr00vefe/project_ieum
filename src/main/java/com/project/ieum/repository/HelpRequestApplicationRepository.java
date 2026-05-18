package com.project.ieum.repository;

import com.project.ieum.entity.request.HelpRequestApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HelpRequestApplicationRepository extends JpaRepository<HelpRequestApplication, Long> {
    boolean existsByHelpRequest_IdAndCaregiver_UserId(Long helpRequestId, Long caregiverId);
}
