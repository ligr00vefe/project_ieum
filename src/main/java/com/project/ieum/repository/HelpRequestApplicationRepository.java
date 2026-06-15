package com.project.ieum.repository;

import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HelpRequestApplicationRepository extends JpaRepository<HelpRequestApplication, Long> {
    boolean existsByHelpRequest_IdAndCaregiver_UserId(Long helpRequestId, Long caregiverId);
    Optional<HelpRequestApplication> findByHelpRequest_IdAndCaregiver_UserId(Long helpRequestId, Long caregiverUserId);

    List<HelpRequestApplication> findByHelpRequest_IdOrderByCreatedAtDesc(Long helpRequestId);

    @EntityGraph(attributePaths = {"helpRequest", "helpRequest.serviceCategory", "helpRequest.requester"})
    List<HelpRequestApplication> findByCaregiver_UserIdOrderByCreatedAtDesc(Long caregiverUserId);

    List<HelpRequestApplication> findByHelpRequest_IdAndStatus(Long helpRequestId, ApplicationStatus status);
    List<HelpRequestApplication> findByHelpRequest_IdAndStatusIn(Long helpRequestId, List<ApplicationStatus> statuses);
    long countByStatus(ApplicationStatus status);
}
