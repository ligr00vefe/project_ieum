package com.project.ieum.controller;

import com.project.ieum.dto.request.ApplyRequest;
import com.project.ieum.service.HelpRequestService;
import com.project.ieum.service.MatchingService;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.recommend.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/caregiver/board")
@RequiredArgsConstructor
public class CaregiverBoardController {

    private final HelpRequestService helpRequestService;
    private final MatchingService matchingService;
    private final RecommendationService recommendationService;
    private final CurrentUserService currentUserService;

    @GetMapping({"", "/"})
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        var requestPage = helpRequestService.getOpenRequests(PageRequest.of(page, 10));
        int totalPages = requestPage.getTotalPages();
        model.addAttribute("title", "매칭 게시판");
        model.addAttribute("requests", requestPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", Math.max(0, page - 2));
        model.addAttribute("endPage", Math.min(totalPages - 1, page + 2));
        model.addAttribute("content", "caregiver/board/list");
        return "layout/layout";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        boolean alreadyApplied = matchingService.hasApplied(id, currentUserId);
        model.addAttribute("title", "게시물 상세");
        model.addAttribute("request", helpRequestService.get(id));
        model.addAttribute("applyRequest", new ApplyRequest());
        model.addAttribute("alreadyApplied", alreadyApplied);
        if (alreadyApplied) {
            matchingService.getMyConversationId(id, currentUserId).ifPresent(cid ->
                model.addAttribute("myConversationId", cid));
        }
        model.addAttribute("handshake", matchingService.getHandshakeView(id, currentUserId));
        model.addAttribute("recommendations", recommendationService.recommendCaregivers(id, 5));
        model.addAttribute("content", "caregiver/board/detail");
        return "layout/layout";
    }

    @PostMapping("/{id}/apply")
    public String apply(
            @PathVariable Long id,
            @Valid @ModelAttribute ApplyRequest applyRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "첫 메시지는 500자 이하로 입력해주세요.");
            return "redirect:/caregiver/board/" + id;
        }
        matchingService.apply(id, applyRequest);
        redirectAttributes.addFlashAttribute("applied", true);
        return "redirect:/caregiver/board/" + id;
    }

    // 활동 시작/종료 양측 확인 — 도우미(선정된 활동지원사) 측.
    @PostMapping("/{id}/confirm-start")
    public String confirmStart(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        matchingService.confirmStart(id);
        redirectAttributes.addFlashAttribute("message", "활동 시작을 확인했습니다.");
        return "redirect:/caregiver/board/" + id;
    }

    @PostMapping("/{id}/confirm-end")
    public String confirmEnd(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        matchingService.confirmEnd(id);
        redirectAttributes.addFlashAttribute("message", "활동 종료를 확인했습니다.");
        return "redirect:/caregiver/board/" + id;
    }
}
