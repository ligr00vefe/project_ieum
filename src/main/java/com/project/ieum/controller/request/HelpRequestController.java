package com.project.ieum.controller.request;

import com.project.ieum.dto.request.ApplyRequest;
import com.project.ieum.dto.request.HelpRequestForm;
import com.project.ieum.service.HelpRequestService;
import com.project.ieum.service.MasterDataService;
import com.project.ieum.service.MatchingService;
import com.project.ieum.service.recommend.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class HelpRequestController {

    private final HelpRequestService helpRequestService;
    private final MatchingService matchingService;
    private final MasterDataService masterDataService;
    private final RecommendationService recommendationService;

    @GetMapping("/requests")
    public String list(@PageableDefault(size = 20) Pageable pageable, Model model) {
        model.addAttribute("title", "매칭 게시판");
        model.addAttribute("requests", helpRequestService.getOpenRequests(pageable));
        return "request/list";
    }

    @GetMapping("/requests/new")
    public String createForm(Model model) {
        prepareForm(model, new HelpRequestForm(), "도움 요청 작성");
        return "request/form";
    }

    @PostMapping("/requests")
    public String create(
            @Valid @ModelAttribute("form") HelpRequestForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, "도움 요청 작성");
            return "request/form";
        }
        var helpRequest = helpRequestService.create(form);
        redirectAttributes.addFlashAttribute("message", "도움 요청이 등록되었습니다.");
        return "redirect:/my/requests/" + helpRequest.getId();
    }

    @GetMapping("/requests/{requestId}")
    public String detail(@PathVariable Long requestId, Model model) {
        model.addAttribute("title", "도움 요청 상세");
        model.addAttribute("request", helpRequestService.get(requestId));
        model.addAttribute("applyRequest", new ApplyRequest());
        model.addAttribute("recommendations", recommendationService.recommendCaregivers(requestId, 5));
        return "request/detail";
    }

    @PostMapping("/requests/{requestId}/apply")
    public String apply(
            @PathVariable Long requestId,
            @Valid @ModelAttribute ApplyRequest applyRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "첫 메시지는 500자 이하로 입력해주세요.");
            return "redirect:/requests/" + requestId;
        }
        matchingService.apply(requestId, applyRequest);
        redirectAttributes.addFlashAttribute("message", "지원이 완료되었습니다. 대화방에서 사전 상담을 이어갈 수 있습니다.");
        return "redirect:/me";
    }

    @GetMapping("/my/requests")
    public String myRequests(Model model) {
        model.addAttribute("title", "내 요청");
        model.addAttribute("requests", helpRequestService.getMyRequests());
        return "request/my-list";
    }

    @GetMapping("/my/requests/{requestId}")
    public String myRequestDetail(@PathVariable Long requestId, Model model) {
        model.addAttribute("title", "내 요청 상세");
        model.addAttribute("request", helpRequestService.get(requestId));
        return "request/my-detail";
    }

    // (이슈 #8 정본) 본문 수정(/edit GET·POST) 엔드포인트는 제거함.
    // HelpRequest는 write-once — 도우미가 본 내용/시간/위치가 지원 후 바뀌면 신뢰성이 깨지므로
    // 생성 후 본문 수정을 막는다. 변경이 필요하면 취소(cancel→CLOSED) 후 새로 작성한다.

    @PostMapping("/my/requests/{requestId}/cancel")
    public String cancel(@PathVariable Long requestId, RedirectAttributes redirectAttributes) {
        helpRequestService.cancel(requestId);
        redirectAttributes.addFlashAttribute("message", "요청이 취소되었습니다.");
        return "redirect:/my/requests";
    }

    // 하드 삭제 — 2단계 삭제의 2단계. CLOSED 상태에서만 게시자가 명시적으로.
    @PostMapping("/my/requests/{requestId}/delete")
    public String delete(@PathVariable Long requestId, RedirectAttributes redirectAttributes) {
        helpRequestService.delete(requestId);
        redirectAttributes.addFlashAttribute("message", "요청이 삭제되었습니다.");
        return "redirect:/my/requests";
    }

    @GetMapping("/my/requests/{requestId}/applications")
    public String applications(@PathVariable Long requestId, Model model) {
        model.addAttribute("title", "지원자 리스트");
        model.addAttribute("request", helpRequestService.get(requestId));
        model.addAttribute("applications", matchingService.getApplicationsForRequest(requestId));
        return "request/applications";
    }

    @PostMapping("/applications/{applicationId}/accept")
    public String accept(@PathVariable Long applicationId, RedirectAttributes redirectAttributes) {
        matchingService.accept(applicationId);
        redirectAttributes.addFlashAttribute("message", "매칭이 확정되었습니다.");
        return "redirect:/my/requests";
    }

    @PostMapping("/applications/{applicationId}/reject")
    public String reject(@PathVariable Long applicationId, RedirectAttributes redirectAttributes) {
        matchingService.reject(applicationId);
        redirectAttributes.addFlashAttribute("message", "지원자를 거절했습니다.");
        return "redirect:/my/requests";
    }

    @PostMapping("/applications/{applicationId}/withdraw")
    public String withdraw(@PathVariable Long applicationId, RedirectAttributes redirectAttributes) {
        matchingService.withdraw(applicationId);
        redirectAttributes.addFlashAttribute("message", "지원을 취소했습니다.");
        return "redirect:/me";
    }

    @PostMapping("/requests/{requestId}/start")
    public String startActivity(@PathVariable Long requestId, RedirectAttributes redirectAttributes) {
        matchingService.startActivity(requestId);
        redirectAttributes.addFlashAttribute("message", "활동이 시작되었습니다.");
        return "redirect:/my/requests/" + requestId;
    }

    @PostMapping("/requests/{requestId}/complete")
    public String completeActivity(@PathVariable Long requestId, RedirectAttributes redirectAttributes) {
        matchingService.completeActivity(requestId);
        redirectAttributes.addFlashAttribute("message", "활동이 완료되었습니다. 후기를 남겨주세요.");
        return "redirect:/reviews/requests/" + requestId + "/new";
    }

    private void prepareForm(Model model, HelpRequestForm form, String title) {
        model.addAttribute("title", title);
        model.addAttribute("form", form);
        model.addAttribute("serviceCategories", masterDataService.getAllServiceCategories());
        model.addAttribute("regions", masterDataService.getAllRegions());
        model.addAttribute("personalityTags", masterDataService.getAllPersonalityTags());
    }
}
