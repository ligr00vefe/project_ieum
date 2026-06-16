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

    // 도움 요청 리스트 — 특정 상태를 기준 좌표(:lat,:lng)에서 가까운 순으로(페이지네이션).
    // 거리 정렬은 acos 없이 "코사인 유사도(구면 내적) 내림차순 = 거리 오름차순"으로 표현한다.
    //   · 함수는 sin/cos만 사용(HQL 표준). 도→라디안은 radians() 대신 리터럴 상수(π/180)를 곱해 dialect 함수 의존을 제거.
    //   · 좌표가 없는(null) 요청은 항상 뒤로(CASE), 동률·좌표없음은 시작시각 오름차순으로 폴백.
    // serviceCategory 동시 로딩(N+1 방지).
    @EntityGraph(attributePaths = {"serviceCategory"})
    @Query(value = """
        select hr from HelpRequest hr
        where hr.status = :status
        order by
          case when hr.latitude is null or hr.longitude is null then 1 else 0 end asc,
          ( sin(:lat * 0.017453292519943295) * sin(hr.latitude * 0.017453292519943295)
          + cos(:lat * 0.017453292519943295) * cos(hr.latitude * 0.017453292519943295)
            * cos(hr.longitude * 0.017453292519943295 - :lng * 0.017453292519943295) ) desc,
          hr.desiredStartDatetime asc
        """,
        countQuery = "select count(hr) from HelpRequest hr where hr.status = :status")
    Page<HelpRequest> findByStatusOrderByDistance(@Param("status") HelpRequestStatus status,
                                                  @Param("lat") double lat,
                                                  @Param("lng") double lng,
                                                  Pageable pageable);

    // 내 도움 요청 관리 — 요청자의 요청을 최근 작성 순으로. serviceCategory 동시 로딩(N+1 방지).
    @EntityGraph(attributePaths = {"serviceCategory"})
    List<HelpRequest> findByRequesterOrderByCreatedAtDesc(UserProfile requester);

    // 전체 활성 요청 목록 — 매칭 게시판 전체 조회용 (OPEN/MATCHED 상태).
    List<HelpRequest> findByStatusInOrderByCreatedAtDesc(List<HelpRequestStatus> statuses);
    Page<HelpRequest> findByStatusInOrderByCreatedAtDesc(List<HelpRequestStatus> statuses, Pageable pageable);

    // 도움 요청 상세 — id로 1건 조회하며 serviceCategory·requester를 함께 로딩(N+1 방지).
    @EntityGraph(attributePaths = {"serviceCategory", "requester"})
    Optional<HelpRequest> getHelpRequestById(Long id);

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
}
