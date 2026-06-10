package com.project.ieum.repository;

import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.user.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long>, com.project.ieum.repository.search.HelpRequestSearchRepository {

    // (#9) DB unique(requester, desired_start_datetime) 제거에 따른 시간대 겹침 검사 지원 조회.
    // 같은 요청자의 활성 요청 중 후보 구간 [start, end)와 시간대가 겹치는 건이 있는지 검사.
    // 겹침 조건: 기존.start < 후보.end  AND  후보.start < coalesce(기존.end, 기존.start)
    // 활성 상태(activeStatuses)만 충돌 대상으로 본다(COMPLETED/CLOSED 제외 권장).
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

    // 도움 요청 리스트 — 특정 상태를 시작시각 오름차순으로(페이지네이션). serviceCategory 동시 로딩(N+1 방지).
    @EntityGraph(attributePaths = {"serviceCategory"})
    Page<HelpRequest> findByStatusOrderByDesiredStartDatetimeAscIdDesc(HelpRequestStatus status, Pageable pageable);

    // 내 도움 요청 관리 — 요청자의 요청을 최근 작성 순으로. serviceCategory 동시 로딩(N+1 방지).
    @EntityGraph(attributePaths = {"serviceCategory"})
    List<HelpRequest> findByRequesterOrderByCreatedAtDesc(UserProfile requester);

    // 전체 활성 요청 목록 — 매칭 게시판 전체 조회용 (OPEN/MATCHED 상태).
    List<HelpRequest> findByStatusInOrderByCreatedAtDesc(List<HelpRequestStatus> statuses);

    // 도움 요청 상세 — id로 1건 조회하며 serviceCategory·requester를 함께 로딩(N+1 방지).
    @EntityGraph(attributePaths = {"serviceCategory", "requester"})
    Optional<HelpRequest> getHelpRequestById(Long id);
}
