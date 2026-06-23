package com.project.ieum.controller.admin;

import com.project.ieum.entity.report.ReportStatus;
import com.project.ieum.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 신고 검토/처리 (#22 +#27 신고처리). 경로 보호는 SecurityConfig의 /admin/** hasRole(ADMIN)로 이미 적용.
 * 대상 정지(BAN)는 별도 액션 없이 기존 회원 상태변경(/admin/users/{id}/status)을 재사용한다.
 */
@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "all") String type,
                       Model model) {
        var reportPage = reportService.getReportsByType(type, PageRequest.of(page, 10));
        model.addAttribute("reports", reportPage.getContent());
        model.addAttribute("reportStatuses", ReportStatus.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reportPage.getTotalPages());
        model.addAttribute("startPage", Math.max(0, page - 2));
        model.addAttribute("endPage", Math.min(reportPage.getTotalPages() - 1, page + 2));
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("title", "신고 관리");
        model.addAttribute("activeType", type);
        return "admin/reports/list";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam ReportStatus status,
                               RedirectAttributes ra) {
        reportService.changeStatus(id, status);
        ra.addFlashAttribute("message", "신고 상태가 변경되었습니다.");
        return "redirect:/admin/reports";
    }
}
