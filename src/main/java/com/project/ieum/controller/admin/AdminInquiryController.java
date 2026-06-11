package com.project.ieum.controller.admin;

import com.project.ieum.dto.admin.InquiryForm;
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
    public String list(@RequestParam(required = false) String status, Model model) {
        var inquiries = (status != null && !status.isBlank())
                ? inquiryService.getByStatus(InquiryStatus.valueOf(status))
                : inquiryService.getAll();
        model.addAttribute("inquiries", inquiries);
        model.addAttribute("selectedStatus", status);
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
}
