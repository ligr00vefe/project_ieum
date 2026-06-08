package com.project.ieum.repository;

import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long> {

    // (#9) 시간대 겹침 검사
    @Query("""
        select (count(hr) > 0) from HelpRequest hr
        where hr.requester = :requester
          and hr.status in :activeStatuses
          and hr.desiredStartDatetime < :end
          and :start < coalesce(hr.desiredEndDatetime, hr.desiredStartDatetime)
        """)
    boolean existsOverlapping(@Param("requester") UserProfile requester,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end,
                              @Param("activeStatuses") List<HelpRequestStatus> activeStatuses);

    List<HelpRequest> findByRequesterOrderByCreatedAtDesc(UserProfile requester);

    Page<HelpRequest> findByStatusOrderByDesiredStartDatetimeAscIdDesc(HelpRequestStatus status, Pageable pageable);
}
