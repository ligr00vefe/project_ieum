package com.project.ieum.service.report;

import com.project.ieum.dto.report.ReportForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.report.Report;
import com.project.ieum.entity.report.ReportReason;
import com.project.ieum.exception.BadRequestException;
import com.project.ieum.repository.ReportRepository;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.service.common.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ReportService} 단위 검증 (Mockito) — 신고 생성의 권한·중복 가드만 본다(영속성은 mock).
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("자기 자신은 신고할 수 없다 — 저장하지 않는다")
    void create_self_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(user(1L));

        ReportForm form = new ReportForm(1L, null, ReportReason.ABUSE, null);

        assertThatThrownBy(() -> reportService.create(form)).isInstanceOf(BadRequestException.class);
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 처리 중인 신고가 있으면 중복으로 거부 — 저장하지 않는다")
    void create_duplicate_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(user(1L));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(reportRepository.existsByReporter_IdAndTarget_IdAndStatusIn(eq(1L), eq(2L), anyCollection()))
                .thenReturn(true);

        ReportForm form = new ReportForm(2L, null, ReportReason.FRAUD, "금전 요구");

        assertThatThrownBy(() -> reportService.create(form)).isInstanceOf(BadRequestException.class);
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("정상 신고는 저장된다")
    void create_valid_saves() {
        when(currentUserService.getCurrentUser()).thenReturn(user(1L));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(reportRepository.existsByReporter_IdAndTarget_IdAndStatusIn(anyLong(), anyLong(), anyCollection()))
                .thenReturn(false);
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReportForm form = new ReportForm(2L, 10L, ReportReason.NO_SHOW, "약속 불이행");
        reportService.create(form);

        verify(reportRepository).save(any(Report.class));
    }

    private User user(Long id) {
        return User.builder().id(id).build();
    }
}
