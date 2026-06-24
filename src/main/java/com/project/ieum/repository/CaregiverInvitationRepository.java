package com.project.ieum.repository;

import com.project.ieum.entity.request.CaregiverInvitation;
import com.project.ieum.entity.request.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaregiverInvitationRepository extends JpaRepository<CaregiverInvitation, Long> {

    boolean existsByHelpRequest_IdAndCaregiver_UserId(Long requestId, Long caregiverId);

    Optional<CaregiverInvitation> findByHelpRequest_IdAndCaregiver_UserId(Long requestId, Long caregiverId);

    List<CaregiverInvitation> findByHelpRequest_IdAndStatus(Long requestId, InvitationStatus status);

    boolean existsByHelpRequest_IdAndStatus(Long requestId, InvitationStatus status);
}
