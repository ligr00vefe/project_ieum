package com.project.ieum.service.report;

import com.project.ieum.dto.report.ReportForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.report.Report;
import com.project.ieum.entity.report.ReportStatus;
import com.project.ieum.exception.BadRequestException;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.ConversationRepository;
import com.project.ieum.repository.ReportRepository;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.repository.market.MarketChatRepository;
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

    private static final List<ReportStatus> OPEN_STATUSES =
            List.of(ReportStatus.RECEIVED, ReportStatus.REVIEWING);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ConversationRepository conversationRepository;
    private final MarketChatRepository marketChatRepository;

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
                .marketChatId(form.marketChatId())
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

        if (status == ReportStatus.RESOLVED) {
            // 연결된 채팅방 자동 종료
            if (report.getConversationId() != null) {
                conversationRepository.findById(report.getConversationId())
                        .ifPresent(c -> c.close());
            }
            if (report.getMarketChatId() != null) {
                marketChatRepository.findById(report.getMarketChatId())
                        .ifPresent(c -> c.close());
            }

            // 신고 3회 누적 시 경고회원(WARNED) 자동 전환
            User target = report.getTarget();
            long resolvedCount = reportRepository.countByTarget_IdAndStatus(target.getId(), ReportStatus.RESOLVED);
            if (resolvedCount >= 3 && target.getStatus() == UserStatus.ACTIVE) {
                target.changeStatus(UserStatus.WARNED);
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<Report> getReportsByType(String type, Pageable pageable) {
        return switch (type) {
            case "matching" -> reportRepository.findByConversationIdIsNotNullOrderByCreatedAtDesc(pageable);
            case "market"   -> reportRepository.findByMarketChatIdIsNotNullOrderByCreatedAtDesc(pageable);
            default         -> reportRepository.findAllByOrderByCreatedAtDesc(pageable);
        };
    }
}
