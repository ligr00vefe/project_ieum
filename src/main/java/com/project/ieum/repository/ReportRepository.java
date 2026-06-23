package com.project.ieum.repository;

import com.project.ieum.entity.report.Report;
import com.project.ieum.entity.report.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);

    // 중복 신고 가드 — 같은 신고자→대상의 미처리(접수/검토중) 신고가 이미 있는지.
    boolean existsByReporter_IdAndTarget_IdAndStatusIn(Long reporterId, Long targetId, Collection<ReportStatus> statuses);

    // 특정 대상의 처리 완료(RESOLVED) 신고 횟수 — 경고 기준 카운팅에 사용.
    long countByTarget_IdAndStatus(Long targetId, ReportStatus status);

    // 관리자 신고 목록 필터용
    Page<Report> findByConversationIdIsNotNullOrderByCreatedAtDesc(Pageable pageable);
    Page<Report> findByMarketChatIdIsNotNullOrderByCreatedAtDesc(Pageable pageable);
}
