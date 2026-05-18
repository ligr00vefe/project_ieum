package com.project.ieum.controller.recommend;

import com.project.ieum.dto.recommend.CaregiverRecommendationResponse;
import com.project.ieum.service.recommend.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/help-requests/{helpRequestId}/caregivers")
    public List<CaregiverRecommendationResponse> caregivers(
            @PathVariable Long helpRequestId,
            @RequestParam(defaultValue = "5") int limit) {
        return recommendationService.recommendCaregivers(helpRequestId, limit);
    }
}
