package com.project.ieum.controller;

import com.project.ieum.dto.request.ApplyRequest;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.service.HelpRequestService;
import com.project.ieum.service.MatchingService;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.geocoding.GeoDistance;
import com.project.ieum.service.geocoding.PlaceResult;
import com.project.ieum.service.geocoding.TmapPlaceSearchService;
import com.project.ieum.service.recommend.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/caregiver/board")
public class CaregiverBoardController {

    private final HelpRequestService helpRequestService;
    private final MatchingService matchingService;
    private final RecommendationService recommendationService;
    private final CurrentUserService currentUserService;
    private final TmapPlaceSearchService placeSearchService;
    // 지도 위치 선택용 TMap JS SDK appKey(클라이언트 노출). 비어 있으면 지도 버튼을 숨긴다.
    private final String tmapAppKey;

    public CaregiverBoardController(HelpRequestService helpRequestService,
                                    MatchingService matchingService,
                                    RecommendationService recommendationService,
                                    CurrentUserService currentUserService,
                                    TmapPlaceSearchService placeSearchService,
                                    @Value("${tmap.app-key:}") String tmapAppKey) {
        this.helpRequestService = helpRequestService;
        this.matchingService = matchingService;
        this.recommendationService = recommendationService;
        this.currentUserService = currentUserService;
        this.placeSearchService = placeSearchService;
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
                       Model model) {
        boolean locationSorted = lat != null && lng != null;
        var requestPage = helpRequestService.getOpenRequests(PageRequest.of(page, 10), lat, lng);
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
        if (locationSorted) {
            model.addAttribute("distanceMap", buildDistanceLabels(requestPage.getContent(), lat, lng));
        }
        model.addAttribute("content", "caregiver/board/list");
        return "layout/layout";
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
