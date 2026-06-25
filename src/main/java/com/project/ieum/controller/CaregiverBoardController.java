package com.project.ieum.controller;

import com.project.ieum.dto.request.ApplyRequest;
import com.project.ieum.dto.search.HelpRequestSearchCondition;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.service.HelpRequestService;
import com.project.ieum.service.MasterDataService;
import com.project.ieum.service.MatchingService;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.geocoding.GeoDistance;
import com.project.ieum.service.geocoding.PlaceResult;
import com.project.ieum.service.geocoding.TmapPlaceSearchService;
import com.project.ieum.service.recommend.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/caregiver/board")
public class CaregiverBoardController {

    private final HelpRequestService helpRequestService;
    private final MatchingService matchingService;
    private final RecommendationService recommendationService;
    private final CurrentUserService currentUserService;
    private final TmapPlaceSearchService placeSearchService;
    private final MasterDataService masterDataService;
    // 지도 위치 선택용 TMap JS SDK appKey(클라이언트 노출). 비어 있으면 지도 버튼을 숨긴다.
    private final String tmapAppKey;

    public CaregiverBoardController(HelpRequestService helpRequestService,
                                    MatchingService matchingService,
                                    RecommendationService recommendationService,
                                    CurrentUserService currentUserService,
                                    TmapPlaceSearchService placeSearchService,
                                    MasterDataService masterDataService,
                                    @Value("${tmap.app-key:}") String tmapAppKey) {
        this.helpRequestService = helpRequestService;
        this.matchingService = matchingService;
        this.recommendationService = recommendationService;
        this.currentUserService = currentUserService;
        this.placeSearchService = placeSearchService;
        this.masterDataService = masterDataService;
        this.tmapAppKey = tmapAppKey;
    }

    // 지도 모달의 장소 검색 — 검색어를 좌표로(서버 프록시, CORS 회피). 결과 목록(JSON)을 반환.
    @GetMapping("/poi-search")
    @ResponseBody
    public java.util.List<PlaceResult> poiSearch(@RequestParam String keyword) {
        return placeSearchService.search(keyword);
    }

    @GetMapping({"", "/"})
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) Double lat,
                       @RequestParam(required = false) Double lng,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String sido,
                       @RequestParam(required = false) String sigungu,
                       @RequestParam(required = false) List<Long> serviceCategoryId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       Model model) {
        // 위치 기반(내 주변/지도) ↔ 지역 필터(시/도·시군구)는 상호 배타.
        // 지역 필터(시/도 또는 시군구)가 있으면 위치 정렬을 해제(좌표 무시) → 시작시각 순 + 거리 배지 미표시.
        // 시군구만 단독으로 와도(직접 URL 등) 지역 필터로 간주해 클라이언트 JS(goTo/mapConfirm)와 동일하게 처리.
        if (hasText(sido) || hasText(sigungu)) {
            lat = null;
            lng = null;
        }
        boolean locationSorted = lat != null && lng != null;

        HelpRequestSearchCondition condition = new HelpRequestSearchCondition();
        condition.setKeyword(keyword);
        condition.setSido(sido);
        condition.setSigungu(sigungu);
        condition.setServiceCategoryIds(serviceCategoryId);
        condition.setFromDate(fromDate);
        condition.setToDate(toDate);

        var requestPage = helpRequestService.searchOpenRequests(condition, PageRequest.of(page, 10), lat, lng);
        int totalPages = requestPage.getTotalPages();
        model.addAttribute("title", "매칭 게시판");
        model.addAttribute("requests", requestPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", Math.max(0, page - 2));
        model.addAttribute("endPage", Math.min(totalPages - 1, page + 2));
        model.addAttribute("locationSorted", locationSorted);
        model.addAttribute("lat", lat);
        model.addAttribute("lng", lng);
        model.addAttribute("tmapAppKey", tmapAppKey);
        model.addAttribute("loadTmapSdk", true);
        if (locationSorted) {
            model.addAttribute("distanceMap", buildDistanceLabels(requestPage.getContent(), lat, lng));
        }
        // 검색 필터 옵션 + 입력값(sticky) — 폼 재표시·페이지네이션 파라미터 유지용.
        model.addAttribute("serviceCategories", masterDataService.getAllServiceCategories());
        model.addAttribute("regionMap", helpRequestService.getOpenRegionOptions());
        model.addAttribute("fKeyword", keyword);
        model.addAttribute("fSido", sido);
        model.addAttribute("fSigungu", sigungu);
        model.addAttribute("fServiceCategoryIds", serviceCategoryId);
        model.addAttribute("fFromDate", fromDate);
        model.addAttribute("fToDate", toDate);
        boolean hasFilter = hasText(keyword) || hasText(sido) || hasText(sigungu)
                || !CollectionUtils.isEmpty(serviceCategoryId) || fromDate != null || toDate != null;
        model.addAttribute("hasFilter", hasFilter);
        // 페이지네이션이 현재 정렬·필터를 유지하도록 base URL을 만들어 넘긴다('&' 또는 '?'로 끝남).
        model.addAttribute("pageBaseUrl",
                buildBaseQuery(lat, lng, keyword, sido, sigungu, serviceCategoryId, fromDate, toDate));
        model.addAttribute("content", "caregiver/board/list");
        return "layout/layout";
    }

    // 페이지네이션 base URL — page 외 모든 정렬·필터 파라미터를 보존(빈 값은 생략, 값은 URL 인코딩).
    private String buildBaseQuery(Double lat, Double lng, String keyword, String sido, String sigungu,
                                  List<Long> serviceCategoryIds, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sb = new StringBuilder("/caregiver/board?");
        appendParam(sb, "lat", lat);
        appendParam(sb, "lng", lng);
        appendParam(sb, "keyword", keyword);
        appendParam(sb, "sido", sido);
        appendParam(sb, "sigungu", sigungu);
        if (serviceCategoryIds != null) {
            for (Long id : serviceCategoryIds) {
                appendParam(sb, "serviceCategoryId", id);
            }
        }
        appendParam(sb, "fromDate", fromDate);
        appendParam(sb, "toDate", toDate);
        return sb.toString();
    }

    private void appendParam(StringBuilder sb, String name, Object value) {
        if (value == null) {
            return;
        }
        String raw = value.toString();
        if (raw.isBlank()) {
            return;
        }
        sb.append(name).append('=')
                .append(URLEncoder.encode(raw, StandardCharsets.UTF_8)).append('&');
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // 각 요청까지의 근사 거리 라벨(요청 id → "약 2.5km"). 좌표 없는 요청은 제외한다.
    // 프라이버시 보호로 정확 거리 대신 0.5km 버킷으로 흐린다(GeoDistance.approxLabel).
    private Map<Long, String> buildDistanceLabels(java.util.List<HelpRequest> requests, double lat, double lng) {
        Map<Long, String> labels = new HashMap<>();
        for (HelpRequest r : requests) {
            if (r.getLatitude() != null && r.getLongitude() != null) {
                double km = GeoDistance.haversineKm(
                        lat, lng, r.getLatitude().doubleValue(), r.getLongitude().doubleValue());
                labels.put(r.getId(), GeoDistance.approxLabel(km));
            }
        }
        return labels;
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
        model.addAttribute("selectedCaregiver", matchingService.isSelectedCaregiver(id, currentUserId));
        model.addAttribute("handshake", matchingService.getHandshakeView(id, currentUserId));
        model.addAttribute("recommendations", recommendationService.recommendCaregivers(id, 5));
        model.addAttribute("tmapAppKey", tmapAppKey);
        model.addAttribute("loadTmapSdk", true);
        matchingService.getMyPendingInvitation(id, currentUserId)
                .ifPresent(inv -> model.addAttribute("myInvitation", inv));
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

    @PostMapping("/applications/{applicationId}/withdraw")
    public String withdraw(@PathVariable Long applicationId,
                           RedirectAttributes redirectAttributes) {
        matchingService.withdraw(applicationId);
        redirectAttributes.addFlashAttribute("message", "지원이 취소되었습니다.");
        return "redirect:/caregiver/mypage";
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public String acceptInvitation(@PathVariable Long invitationId,
                                   RedirectAttributes redirectAttributes) {
        Long conversationId = matchingService.acceptInvitation(invitationId);
        return "redirect:/chat/conversations/" + conversationId;
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public String rejectInvitation(@PathVariable Long invitationId,
                                   @RequestParam Long requestId,
                                   RedirectAttributes redirectAttributes) {
        matchingService.rejectInvitation(invitationId);
        redirectAttributes.addFlashAttribute("message", "초대를 거절했습니다.");
        return "redirect:/caregiver/board/" + requestId;
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
