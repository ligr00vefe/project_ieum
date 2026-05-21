package com.project.ieum.repository;

import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.repository.search.HelpRequestSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long>, HelpRequestSearchRepository {

    boolean existsByRequester_UserIdAndDesiredDateAndStatusNot(Long requesterUserId, LocalDate desiredDate, com.project.ieum.entity.request.HelpRequestStatus status);

    List<HelpRequest> findByRequester_UserIdOrderByCreatedAtDesc(Long requesterUserId);

    Page<HelpRequest> findByStatusOrderByDesiredDateAscIdDesc(com.project.ieum.entity.request.HelpRequestStatus status, Pageable pageable);
}
