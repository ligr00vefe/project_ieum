package com.project.ieum.controller;

import com.project.ieum.dto.request.HelpRequestForm;
import com.project.ieum.dto.search.HelpRequestSearchCondition;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.request.HelpRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.project.ieum.repository.HelpRequestApplicationRepository;
import com.project.ieum.entity.request.InvitationStatus;
import com.project.ieum.repository.CaregiverInvitationRepository;
import com.project.ieum.service.HelpRequestService;
import com.project.ieum.service.MasterDataService;
import com.project.ieum.service.MatchingService;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.recommend.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/disabled/board")
public class DisabledBoardController {

    private final HelpRequestService helpRequestService;
    private final MatchingService matchingService;
    private final MasterDataService masterDataService;
    private final CurrentUserService currentUserService;
    private final HelpRequestApplicationRepository applicationRepository;
    private final RecommendationService recommendationService;
    private final CaregiverInvitationRepository caregiverInvitationRepository;
    private final String tmapAppKey;

    public DisabledBoardController(HelpRequestService helpRequestService,
                                   MatchingService matchingService,
                                   MasterDataService masterDataService,
                                   CurrentUserService currentUserService,
                                   HelpRequestApplicationRepository applicationRepository,
                                   RecommendationService recommendationService,
                                   CaregiverInvitationRepository caregiverInvitationRepository,
                                   @Value("${tmap.app-key:}") String tmapAppKey) {
        this.helpRequestService = helpRequestService;
        this.matchingService = matchingService;
        this.masterDataService = masterDataService;
        this.currentUserService = currentUserService;
        this.applicationRepository = applicationRepository;
        this.recommendationService = recommendationService;
        this.caregiverInvitationRepository = caregiverInvitationRepository;
        this.tmapAppKey = tmapAppKey;
    }

    @GetMapping({"", "/"})
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) List<Long> serviceCategoryId,
                       Model model) {
        // 이용자 둘러보기 보드 — 남의 OPEN + 내 모든 상태(이슈11 골조버그 ④ 역전, ADR 2026-06-21).
        // 본인 요청 관리 동선은 마이페이지에 완비되어 보드를 둘러보기 surface로 전환한다.
        HelpRequestSearchCondition condition = new HelpRequestSearchCondition();
        condition.setKeyword(keyword);
        condition.setServiceCategoryIds(serviceCategoryId);

        Page<HelpRequest> requests = helpRequestService.searchBoardForUser(condition, PageRequest.of(page, 10));
        int totalPages = requests.getTotalPages();

        model.addAttribute("title", "매칭 게시판");
        model.addAttribute("requests", requests);
        model.addAttribute("currentUserId", currentUserService.getCurrentUser().getId());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", Math.max(0, page - 2));
        model.addAttribute("endPage", Math.min(totalPages - 1, page + 2));
        model.addAttribute("serviceCategories", masterDataService.getAllServiceCategories());
        model.addAttribute("fKeyword", keyword);
        model.addAttribute("fServiceCategoryIds", serviceCategoryId);
        model.addAttribute("pageBaseUrl", buildBoardBaseUrl(keyword, serviceCategoryId));
        model.addAttribute("content", "disabled/board/list");
        return "layout/layout";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("title", "요청 글 작성");
        model.addAttribute("form", new HelpRequestForm());
        model.addAttribute("serviceCategories", masterDataService.getAllServiceCategories());
        model.addAttribute("personalityTags", masterDataService.getCaregiverPersonalityTags());
        model.addAttribute("tmapAppKey", tmapAppKey);
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
            model.addAttribute("tmapAppKey", tmapAppKey);
            model.addAttribute("content", "disabled/board/create");
            return "layout/layout";
        }
        var helpRequest = helpRequestService.create(form);
        redirectAttributes.addFlashAttribute("message", "도움 요청이 등록되었습니다.");
        return "redirect:/disabled/board/" + helpRequest.getId();
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("title", "요청 글 수정");
        model.addAttribute("form", helpRequestService.buildEditForm(id));
        model.addAttribute("editMode", true);
        model.addAttribute("requestId", id);
        model.addAttribute("serviceCategories", masterDataService.getAllServiceCategories());
        model.addAttribute("personalityTags", masterDataService.getCaregiverPersonalityTags());
        model.addAttribute("tmapAppKey", tmapAppKey);
        model.addAttribute("content", "disabled/board/create");
        return "layout/layout";
    }

    @PostMapping("/{id}/edit")
    public String editSubmit(@PathVariable Long id,
                             @Valid @ModelAttribute("form") HelpRequestForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "요청 글 수정");
            model.addAttribute("editMode", true);
            model.addAttribute("requestId", id);
            model.addAttribute("serviceCategories", masterDataService.getAllServiceCategories());
            model.addAttribute("personalityTags", masterDataService.getCaregiverPersonalityTags());
            model.addAttribute("tmapAppKey", tmapAppKey);
            model.addAttribute("content", "disabled/board/create");
            return "layout/layout";
        }
        try {
            helpRequestService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "요청 글이 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/disabled/board/" + id;
    }

    @GetMapping("/{id}/repost")
    public String repostForm(@PathVariable Long id, Model model) {
        model.addAttribute("title", "요청 글 작성");
        model.addAttribute("form", helpRequestService.buildRepostForm(id));
        model.addAttribute("serviceCategories", masterDataService.getAllServiceCategories());
        model.addAttribute("personalityTags", masterDataService.getCaregiverPersonalityTags());
        model.addAttribute("tmapAppKey", tmapAppKey);
        model.addAttribute("content", "disabled/board/create");
        return "layout/layout";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var currentUser = currentUserService.getCurrentUser();
        Long currentUserId = currentUser.getId();
        HelpRequest request = helpRequestService.get(id);
        model.addAttribute("title", "게시물 상세");
        model.addAttribute("request", request);
        model.addAttribute("applicationCount", matchingService.countApplicationsForRequest(id));
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("isAdmin", currentUser.getRole() == UserRole.ADMIN);
        model.addAttribute("handshake", matchingService.getHandshakeView(id, currentUserId));
        matchingService.getMatchedParty(id)
                .ifPresent(party -> model.addAttribute("matchedParty", party));
        model.addAttribute("tmapAppKey", tmapAppKey);
        // 추천 활동지원사 — OPEN 상태 + 본인 게시물일 때만 노출
        boolean isOwner = request.getRequester().getUserId().equals(currentUserId);
        model.addAttribute("hasPendingInvitation", false);
        if (request.getStatus() == HelpRequestStatus.OPEN && isOwner) {
            model.addAttribute("recommendations", recommendationService.recommendCaregivers(id, 5));
            model.addAttribute("hasPendingInvitation",
                    caregiverInvitationRepository.existsByHelpRequest_IdAndStatus(id, InvitationStatus.PENDING));
        }
        // 활동지원사가 보는 경우 — 초대 수락/거절 버튼용
        if (currentUser.getRole() == UserRole.CAREGIVER) {
            matchingService.getMyPendingInvitation(id, currentUserId)
                    .ifPresent(inv -> model.addAttribute("myInvitation", inv));
        }
        model.addAttribute("content", "disabled/board/detail");
        return "layout/layout";
    }

    @GetMapping("/{id}/applicants")
    public String applicants(@PathVariable Long id,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        var currentUser = currentUserService.getCurrentUser();
        List<HelpRequestApplication> applications = (currentUser.getRole() == UserRole.ADMIN)
                ? applicationRepository.findByHelpRequest_IdOrderByCreatedAtDesc(id)
                : matchingService.getApplicationsForRequest(id);
        var matchPercentMap = matchingService.getMatchPercentMap(id, applications);
        var sorted = applications.stream()
                .sorted(java.util.Comparator.comparingInt(
                        (com.project.ieum.entity.request.HelpRequestApplication a) ->
                                matchPercentMap.getOrDefault(a.getId(), 0)).reversed())
                .toList();
        int pageSize = 10;
        int totalItems = sorted.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int from = Math.min(page * pageSize, totalItems);
        int to = Math.min(from + pageSize, totalItems);
        var pagedList = sorted.subList(from, to);

        model.addAttribute("title", "지원자 리스트");
        model.addAttribute("request", helpRequestService.get(id));
        model.addAttribute("applications", pagedList);
        model.addAttribute("matchPercentMap", matchPercentMap);
        model.addAttribute("matchTagDetailMap", matchingService.getMatchTagDetailMap(id, sorted));
        model.addAttribute("conversationIdMap", matchingService.getConversationIdMap(sorted));
        model.addAttribute("hasPendingInvitation",
                caregiverInvitationRepository.existsByHelpRequest_IdAndStatus(id, InvitationStatus.PENDING));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", Math.max(0, page - 2));
        model.addAttribute("endPage", Math.min(totalPages - 1, page + 2));
        model.addAttribute("content", "board/applicants");
        return "layout/layout";
    }

    @PostMapping("/{id}/invite/{caregiverId}")
    public String invite(@PathVariable Long id,
                         @PathVariable Long caregiverId,
                         RedirectAttributes redirectAttributes) {
        matchingService.invite(id, caregiverId);
        redirectAttributes.addFlashAttribute("message", "활동지원사에게 초대를 보냈습니다.");
        return "redirect:/disabled/board/" + id;
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

    // 페이지 이동 시 검색조건(keyword, serviceCategoryId 다중)을 유지하는 쿼리스트링 prefix('?' 또는 '&'로 끝남).
    private String buildBoardBaseUrl(String keyword, List<Long> serviceCategoryIds) {
        StringBuilder sb = new StringBuilder("/disabled/board?");
        if (keyword != null && !keyword.isBlank()) {
            sb.append("keyword=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8)).append('&');
        }
        if (serviceCategoryIds != null) {
            for (Long id : serviceCategoryIds) {
                sb.append("serviceCategoryId=").append(id).append('&');
            }
        }
        return sb.toString();
    }
}
