package com.project.ieum.controller;

import com.project.ieum.dto.request.HelpRequestForm;
import com.project.ieum.service.HelpRequestService;
import com.project.ieum.service.MasterDataService;
import com.project.ieum.service.MatchingService;
import com.project.ieum.service.common.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/disabled/board")
@RequiredArgsConstructor
public class DisabledBoardController {

    private final HelpRequestService helpRequestService;
    private final MatchingService matchingService;
    private final MasterDataService masterDataService;
    private final CurrentUserService currentUserService;

    @GetMapping({"", "/"})
    public String list(Model model) {
        model.addAttribute("title", "매칭 게시판");
        model.addAttribute("requests", helpRequestService.getMyRequests());
        model.addAttribute("currentUserId", currentUserService.getCurrentUser().getId());
        model.addAttribute("content", "disabled/board/list");
        return "layout/layout";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("title", "요청 글 작성");
        model.addAttribute("form", new HelpRequestForm());
        model.addAttribute("serviceCategories", masterDataService.getAllServiceCategories());
        model.addAttribute("personalityTags", masterDataService.getCaregiverPersonalityTags());
        model.addAttribute("content", "disabled/board/create");
        return "layout/layout";
    }

    @PostMapping("/create")
    public String createSubmit(
            @Valid @ModelAttribute("form") HelpRequestForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "요청 글 작성");
            model.addAttribute("serviceCategories", masterDataService.getAllServiceCategories());
            model.addAttribute("personalityTags", masterDataService.getCaregiverPersonalityTags());
            model.addAttribute("content", "disabled/board/create");
            return "layout/layout";
        }
        var helpRequest = helpRequestService.create(form);
        redirectAttributes.addFlashAttribute("message", "도움 요청이 등록되었습니다.");
        return "redirect:/disabled/board/" + helpRequest.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        model.addAttribute("title", "게시물 상세");
        model.addAttribute("request", helpRequestService.get(id));
        model.addAttribute("applicationCount", matchingService.countApplicationsForRequest(id));
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("handshake", matchingService.getHandshakeView(id, currentUserId));
        matchingService.getMatchedParty(id)
                .ifPresent(party -> model.addAttribute("matchedParty", party));
        model.addAttribute("content", "disabled/board/detail");
        return "layout/layout";
    }

    @GetMapping("/{id}/applicants")
    public String applicants(@PathVariable Long id, Model model) {
        var applications = matchingService.getApplicationsForRequest(id);
        var matchPercentMap = matchingService.getMatchPercentMap(id, applications);
        var sorted = applications.stream()
                .sorted(java.util.Comparator.comparingInt(
                        (com.project.ieum.entity.request.HelpRequestApplication a) ->
                                matchPercentMap.getOrDefault(a.getId(), 0)).reversed())
                .toList();
        model.addAttribute("title", "지원자 리스트");
        model.addAttribute("request", helpRequestService.get(id));
        model.addAttribute("applications", sorted);
        model.addAttribute("matchPercentMap", matchPercentMap);
        model.addAttribute("matchTagDetailMap", matchingService.getMatchTagDetailMap(id, sorted));
        model.addAttribute("conversationIdMap", matchingService.getConversationIdMap(sorted));
        model.addAttribute("content", "board/applicants");
        return "layout/layout";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        helpRequestService.cancel(id);
        redirectAttributes.addFlashAttribute("message", "도움 요청이 삭제되었습니다.");
        return "redirect:/disabled/board";
    }

    @PostMapping("/applications/{applicationId}/chat")
    public String acceptAndChat(@PathVariable Long applicationId,
                                @RequestParam Long requestId) {
        Long conversationId = matchingService.acceptAndGetConversationId(applicationId);
        return "redirect:/chat/conversations/" + conversationId;
    }

    @PostMapping("/applications/{applicationId}/cancel-match")
    public String cancelMatch(@PathVariable Long applicationId,
                              @RequestParam Long requestId,
                              RedirectAttributes redirectAttributes) {
        matchingService.cancelMatch(applicationId);
        redirectAttributes.addFlashAttribute("message", "매칭이 취소되었습니다.");
        return "redirect:/disabled/board/" + requestId + "/applicants";
    }

    @PostMapping("/applications/{applicationId}/accept")
    public String accept(@PathVariable Long applicationId,
                         @RequestParam Long requestId,
                         RedirectAttributes redirectAttributes) {
        matchingService.accept(applicationId);
        redirectAttributes.addFlashAttribute("message", "매칭이 확정되었습니다.");
        return "redirect:/disabled/board/" + requestId;
    }

    @PostMapping("/applications/{applicationId}/reject")
    public String reject(@PathVariable Long applicationId,
                         @RequestParam Long requestId,
                         RedirectAttributes redirectAttributes) {
        matchingService.reject(applicationId);
        redirectAttributes.addFlashAttribute("message", "지원자를 거절했습니다.");
        return "redirect:/disabled/board/" + requestId + "/applicants";
    }

    // 활동 시작/종료 양측 확인 — 이용자(요청자) 측. 양측이 모두 확인하면 서비스가 상태를 전이한다.
    @PostMapping("/{id}/confirm-start")
    public String confirmStart(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        matchingService.confirmStart(id);
        redirectAttributes.addFlashAttribute("message", "활동 시작을 확인했습니다.");
        return "redirect:/disabled/board/" + id;
    }

    @PostMapping("/{id}/confirm-end")
    public String confirmEnd(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        matchingService.confirmEnd(id);
        redirectAttributes.addFlashAttribute("message", "활동 종료를 확인했습니다.");
        return "redirect:/disabled/board/" + id;
    }
}
