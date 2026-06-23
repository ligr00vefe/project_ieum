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
}
