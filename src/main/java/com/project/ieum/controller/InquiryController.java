package com.project.ieum.controller;

import com.project.ieum.dto.InquiryCreateForm;
import com.project.ieum.dto.admin.InquiryForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.inquiry.Inquiry;
import com.project.ieum.entity.inquiry.InquiryCategory;
import org.springframework.data.domain.Page;
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
@RequestMapping("/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final CurrentUserService currentUserService;

    /**
     * 전체 문의 목록 — 카테고리 필터 + 페이지네이션
     * 비밀글 마스킹은 템플릿에서 처리
     */
    @GetMapping
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        User currentUser = currentUserService.getCurrentUser();
        InquiryCategory cat = null;
        try {
            if (category != null && !category.isBlank()) cat = InquiryCategory.valueOf(category);
        } catch (IllegalArgumentException ignored) { category = null; }

        Page<Inquiry> inquiryPage = inquiryService.getAllPaged(cat, page);
        int totalPages = inquiryPage.getTotalPages();
        int startPage  = Math.max(0, page - 2);
        int endPage    = Math.min(totalPages - 1, page + 2);

        model.addAttribute("title", "문의");
        model.addAttribute("inquiries", inquiryPage.getContent());
        model.addAttribute("inquiryPage", inquiryPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("isAdmin", currentUser.getRole() == UserRole.ADMIN);
        model.addAttribute("categories", InquiryCategory.values());
        model.addAttribute("content", "inquiries/list");
        return "layout/layout";
    }

    /** 문의 작성 폼 */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("title", "문의하기");
        model.addAttribute("form", new InquiryCreateForm());
        model.addAttribute("categories", InquiryCategory.values());
        model.addAttribute("content", "inquiries/form");
        return "layout/layout";
    }

    /** 문의 제출 */
    @PostMapping("/new")
    public String submit(@Valid @ModelAttribute("form") InquiryCreateForm form,
                         BindingResult br, Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("title", "문의하기");
            model.addAttribute("categories", InquiryCategory.values());
            model.addAttribute("content", "inquiries/form");
            return "layout/layout";
        }
        User currentUser = currentUserService.getCurrentUser();
        Inquiry inquiry = inquiryService.create(form, currentUser);
        ra.addFlashAttribute("message", "문의가 등록되었습니다.");
        return "redirect:/inquiries/" + inquiry.getId();
    }

    /**
     * 문의 상세
     * - 비밀글: 본인·관리자 외 접근 차단
     * - 관리자: replyForm 전달 (상세 페이지에서 바로 답변 가능)
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User currentUser = currentUserService.getCurrentUser();
        Inquiry inquiry = inquiryService.getById(id);

        boolean isOwner = inquiry.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (inquiry.getIsSecret() && !isOwner && !isAdmin) {
            ra.addFlashAttribute("error", "비밀글은 작성자 본인만 조회할 수 있습니다.");
            return "redirect:/inquiries";
        }

        model.addAttribute("title", inquiry.getTitle());
        model.addAttribute("inquiry", inquiry);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isAdmin", isAdmin);
        if (isAdmin) {
            model.addAttribute("replyForm", new InquiryForm());
        }
        model.addAttribute("content", "inquiries/detail");
        return "layout/layout";
    }

    /** 관리자 — 클라이언트 상세 페이지에서 답변 등록/수정 */
    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id,
                        @Valid @ModelAttribute("replyForm") InquiryForm form,
                        BindingResult br, Model model,
                        RedirectAttributes ra) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            return "redirect:/inquiries/" + id;
        }
        if (br.hasErrors()) {
            Inquiry inquiry = inquiryService.getById(id);
            model.addAttribute("inquiry", inquiry);
            model.addAttribute("isOwner", false);
            model.addAttribute("isAdmin", true);
            model.addAttribute("content", "inquiries/detail");
            return "layout/layout";
        }
        inquiryService.reply(id, form, currentUser);
        ra.addFlashAttribute("message", "답변이 등록되었습니다.");
        return "redirect:/inquiries/" + id;
    }

    /** 수정 폼 — 본인만 접근 가능 */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User currentUser = currentUserService.getCurrentUser();
        Inquiry inquiry = inquiryService.getById(id);

        if (!inquiry.getAuthor().getId().equals(currentUser.getId())) {
            ra.addFlashAttribute("error", "본인의 문의만 수정할 수 있습니다.");
            return "redirect:/inquiries";
        }

        InquiryCreateForm form = new InquiryCreateForm();
        form.setTitle(inquiry.getTitle());
        form.setBody(inquiry.getBody());
        form.setCategory(inquiry.getCategory());
        form.setSecret(inquiry.getIsSecret());

        model.addAttribute("title", "문의 수정");
        model.addAttribute("form", form);
        model.addAttribute("inquiry", inquiry);
        model.addAttribute("categories", InquiryCategory.values());
        model.addAttribute("content", "inquiries/edit");
        return "layout/layout";
    }

    /** 수정 처리 */
    @PostMapping("/{id}/edit")
    public String editSubmit(@PathVariable Long id,
                             @Valid @ModelAttribute("form") InquiryCreateForm form,
                             BindingResult br, Model model,
                             RedirectAttributes ra) {
        if (br.hasErrors()) {
            Inquiry inquiry = inquiryService.getById(id);
            model.addAttribute("title", "문의 수정");
            model.addAttribute("inquiry", inquiry);
            model.addAttribute("categories", InquiryCategory.values());
            model.addAttribute("content", "inquiries/edit");
            return "layout/layout";
        }
        User currentUser = currentUserService.getCurrentUser();
        inquiryService.update(id, form, currentUser);
        ra.addFlashAttribute("message", "문의가 수정되었습니다.");
        return "redirect:/inquiries/" + id;
    }

    /** 삭제 처리 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        User currentUser = currentUserService.getCurrentUser();
        inquiryService.delete(id, currentUser);
        ra.addFlashAttribute("message", "문의가 삭제되었습니다.");
        return "redirect:/inquiries";
    }
}
