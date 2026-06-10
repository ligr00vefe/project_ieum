package com.project.ieum.repository;

import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpRequestApplicationRepository extends JpaRepository<HelpRequestApplication, Long> {
    boolean existsByHelpRequest_IdAndCaregiver_UserId(Long helpRequestId, Long caregiverId);

    List<HelpRequestApplication> findByHelpRequest_IdOrderByCreatedAtDesc(Long helpRequestId);

    List<HelpRequestApplication> findByCaregiver_UserIdOrderByCreatedAtDesc(Long caregiverUserId);

    List<HelpRequestApplication> findByHelpRequest_IdAndStatus(Long helpRequestId, ApplicationStatus status);
}
