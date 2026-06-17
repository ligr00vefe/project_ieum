package com.project.ieum.controller.admin;

import com.project.ieum.dto.admin.InquiryForm;
import com.project.ieum.entity.inquiry.InquiryCategory;
import com.project.ieum.entity.inquiry.InquiryStatus;
import com.project.ieum.service.admin.InquiryService;
import com.project.ieum.service.common.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        InquiryStatus statusEnum = null;
        InquiryCategory categoryEnum = null;
        try {
            if (status != null && !status.isBlank()) statusEnum = InquiryStatus.valueOf(status);
            if (category != null && !category.isBlank()) categoryEnum = InquiryCategory.valueOf(category);
        } catch (IllegalArgumentException ignored) {}

        var inquiryPage = inquiryService.getFilteredPaged(categoryEnum, statusEnum, keyword, page);
        int totalPages = inquiryPage.getTotalPages();
        int startPage  = Math.max(0, page - 2);
        int endPage    = Math.min(totalPages - 1, page + 2);

        model.addAttribute("inquiries", inquiryPage.getContent());
        model.addAttribute("inquiryPage", inquiryPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categories", InquiryCategory.values());
        model.addAttribute("activeMenu", "inquiries");
        model.addAttribute("title", "문의 관리");
        return "admin/inquiries/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("inquiry", inquiryService.getById(id));
        model.addAttribute("replyForm", new InquiryForm());
        model.addAttribute("activeMenu", "inquiries");
        model.addAttribute("title", "문의 상세");
        return "admin/inquiries/detail";
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id,
                        @Valid @ModelAttribute("replyForm") InquiryForm form,
                        BindingResult br, Model model,
                        RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("inquiry", inquiryService.getById(id));
            model.addAttribute("activeMenu", "inquiries");
            return "admin/inquiries/detail";
        }
        inquiryService.reply(id, form, currentUserService.getCurrentUser());
        ra.addFlashAttribute("message", "답변이 등록되었습니다.");
        return "redirect:/admin/inquiries/" + id;
    }

    /** 답변 상태 PENDING ↔ ANSWERED 토글 */
    @PostMapping("/{id}/status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes ra) {
        inquiryService.toggleStatus(id);
        ra.addFlashAttribute("message", "답변 상태가 변경되었습니다.");
        return "redirect:/admin/inquiries/" + id;
    }

    /** 관리자 문의 삭제 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        inquiryService.delete(id, currentUserService.getCurrentUser());
        ra.addFlashAttribute("message", "문의가 삭제되었습니다.");
        return "redirect:/admin/inquiries";
    }
}
