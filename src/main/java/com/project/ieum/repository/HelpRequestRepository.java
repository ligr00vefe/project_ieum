package com.project.ieum.repository;

import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long> {

    // (#9) DB unique(requester, desired_start_datetime) 제거에 따른 시간대 겹침 검사 지원 조회.
    // 같은 요청자의 활성 요청 중 후보 구간 [start, end)와 시간대가 겹치는 건이 있는지 검사.
    // 겹침 조건: 기존.start < 후보.end  AND  후보.start < coalesce(기존.end, 기존.start)
    // 활성 상태(activeStatuses)만 충돌 대상으로 본다(COMPLETED/CANCELLED/CLOSED 제외 권장).
    // TODO(HelpRequestService): 생성/수정 시 이 메서드로 겹침을 확인하고 도메인 예외 발행.
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
}
