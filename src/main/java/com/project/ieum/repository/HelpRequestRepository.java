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

    // 게시판 지역 필터 옵션 — 특정 상태 요청에 실제 존재하는 (시/도, 시군구) distinct 쌍.
    // 빈 결과만 내는 필터를 막기 위해 정적 지역표가 아닌 실데이터에서 뽑는다.
    @Query("""
        select distinct new com.project.ieum.dto.search.RegionOption(hr.sido, hr.sigungu)
        from HelpRequest hr
        where hr.status = :status and hr.sido is not null and hr.sigungu is not null
        order by hr.sido asc, hr.sigungu asc
        """)
    List<com.project.ieum.dto.search.RegionOption> findDistinctRegionsByStatus(@Param("status") HelpRequestStatus status);

    // 전체 활성 요청 목록 — 매칭 게시판 전체 조회용 (OPEN/MATCHED 상태).
    List<HelpRequest> findByStatusInOrderByCreatedAtDesc(List<HelpRequestStatus> statuses);
    Page<HelpRequest> findByStatusInOrderByCreatedAtDesc(List<HelpRequestStatus> statuses, Pageable pageable);

    // 도움 요청 상세 — id로 1건 조회하며 serviceCategory·requester를 함께 로딩(N+1 방지).
    @EntityGraph(attributePaths = {"serviceCategory", "requester"})
    Optional<HelpRequest> getHelpRequestById(Long id);

    // 관리자 매칭 상세 — JPQL 명시적 JOIN FETCH로 안전하게 로딩
    @Query("SELECT r FROM HelpRequest r LEFT JOIN FETCH r.requester LEFT JOIN FETCH r.serviceCategory WHERE r.id = :id")
    Optional<HelpRequest> findAdminDetail(@Param("id") Long id);

    long countByStatus(HelpRequestStatus status);
    @Query("SELECT r FROM HelpRequest r LEFT JOIN FETCH r.requester LEFT JOIN FETCH r.serviceCategory ORDER BY r.createdAt DESC")
    List<HelpRequest> findAllByOrderByCreatedAtDesc();

    @Query(value = "SELECT r FROM HelpRequest r LEFT JOIN FETCH r.requester LEFT JOIN FETCH r.serviceCategory ORDER BY r.createdAt DESC",
           countQuery = "SELECT count(r) FROM HelpRequest r")
    Page<HelpRequest> findAllPagedWithFetch(Pageable pageable);

    @Query(value = "SELECT r FROM HelpRequest r ORDER BY r.createdAt DESC",
           countQuery = "SELECT count(r) FROM HelpRequest r")
    Page<HelpRequest> findAllPagedAdmin(Pageable pageable);

    Page<HelpRequest> findByStatusOrderByCreatedAtDesc(HelpRequestStatus status, Pageable pageable);
    List<HelpRequest> findTop5ByStatusOrderByUpdatedAtDesc(HelpRequestStatus status);

    // 스케줄러 시간 기반 자동전이용 — 상태 + 임계 일시 조회.
    //   OPEN 만료(희망시작 < now+1h) / MATCHED 노쇼(희망시작 < now-30m) → desiredStartDatetimeBefore
    //   IN_PROGRESS 자동완료(희망종료 < now-30m) → desiredEndDatetimeBefore (null end는 자연히 제외)
    List<HelpRequest> findByStatusAndDesiredStartDatetimeBefore(HelpRequestStatus status, LocalDateTime threshold);
    List<HelpRequest> findByStatusAndDesiredEndDatetimeBefore(HelpRequestStatus status, LocalDateTime threshold);

    @Query(value = "SELECT r FROM HelpRequest r LEFT JOIN FETCH r.requester ORDER BY r.createdAt DESC",
           countQuery = "SELECT count(r) FROM HelpRequest r")
    Page<HelpRequest> findAllPagedAdminWithRequester(Pageable pageable);

    @Query(value = "SELECT r FROM HelpRequest r LEFT JOIN FETCH r.requester WHERE r.status = :status ORDER BY r.createdAt DESC",
           countQuery = "SELECT count(r) FROM HelpRequest r WHERE r.status = :status")
    Page<HelpRequest> findByStatusPagedAdminWithRequester(@Param("status") HelpRequestStatus status, Pageable pageable);

    @Query(value = "SELECT r FROM HelpRequest r LEFT JOIN FETCH r.requester WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY r.createdAt DESC",
           countQuery = "SELECT count(r) FROM HelpRequest r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<HelpRequest> findByTitleContainingAdmin(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT r FROM HelpRequest r LEFT JOIN FETCH r.requester WHERE r.status = :status AND LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY r.createdAt DESC",
           countQuery = "SELECT count(r) FROM HelpRequest r WHERE r.status = :status AND LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<HelpRequest> findByStatusAndTitleContainingAdmin(@Param("status") HelpRequestStatus status, @Param("keyword") String keyword, Pageable pageable);
}
