package com.project.ieum.service.report;

import com.project.ieum.dto.report.ReportForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.report.Report;
import com.project.ieum.entity.report.ReportStatus;
import com.project.ieum.exception.BadRequestException;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.ReportRepository;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.service.common.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    // 미처리 신고로 보는 상태 — 같은 대상 중복 신고 가드 기준.
    private static final List<ReportStatus> OPEN_STATUSES =
            List.of(ReportStatus.RECEIVED, ReportStatus.REVIEWING);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public Report create(ReportForm form) {
        User reporter = currentUserService.getCurrentUser();
        if (reporter.getId().equals(form.targetUserId())) {
            throw new BadRequestException("자기 자신은 신고할 수 없습니다.");
        }
        User target = userRepository.findById(form.targetUserId())
                .orElseThrow(() -> new NotFoundException("신고 대상을 찾을 수 없습니다."));
        if (reportRepository.existsByReporter_IdAndTarget_IdAndStatusIn(
                reporter.getId(), target.getId(), OPEN_STATUSES)) {
            throw new BadRequestException("이미 처리 중인 신고가 있습니다.");
        }
        return reportRepository.save(Report.builder()
                .reporter(reporter)
                .target(target)
                .reason(form.reason())
                .detail(form.detail())
                .conversationId(form.conversationId())
                .status(ReportStatus.RECEIVED)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<Report> getReports(Pageable pageable) {
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public void changeStatus(Long reportId, ReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("신고를 찾을 수 없습니다."));
        report.changeStatus(status);
    }
}
