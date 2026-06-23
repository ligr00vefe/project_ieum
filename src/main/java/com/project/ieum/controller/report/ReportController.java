package com.project.ieum.controller.report;

import com.project.ieum.dto.report.ReportForm;
import com.project.ieum.service.report.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 사용자 신고 제출. 대화방/프로필의 신고 폼(POST)을 받아 접수하고 원래 화면으로 되돌린다.
 * 신고 버튼·모달 마크업은 채팅 프론트(묶음 C, chat/room.html)가 담당하며 이 엔드포인트로 제출한다.
 */
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public String submit(@Valid @ModelAttribute ReportForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        // 신고 맥락에 따라 원래 채팅방으로 되돌린다(오픈 리다이렉트 방지: 내부 경로만).
        String back;
        if (form.conversationId() != null) {
            back = "/chat/conversations/" + form.conversationId();
        } else if (form.marketChatId() != null) {
            back = "/market/chat/" + form.marketChatId();
        } else {
            back = "/";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("reportError", "신고 정보를 다시 확인해 주세요.");
            return "redirect:" + back;
        }
        reportService.create(form);
        redirectAttributes.addFlashAttribute("reportMessage", "신고가 접수되었습니다. 관리자가 검토합니다.");
        return "redirect:" + back;
    }
}
