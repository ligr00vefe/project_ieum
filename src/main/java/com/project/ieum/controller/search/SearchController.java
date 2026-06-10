package com.project.ieum.controller.search;

import com.project.ieum.dto.search.CaregiverSearchCondition;
import com.project.ieum.dto.search.CaregiverSearchResponse;
import com.project.ieum.dto.search.HelpRequestSearchCondition;
import com.project.ieum.dto.search.HelpRequestSearchResponse;
import com.project.ieum.service.search.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/caregivers")
    public Page<CaregiverSearchResponse> caregivers(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Short dayOfWeek,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Boolean hasCertification,
            Pageable pageable) {

        CaregiverSearchCondition condition = new CaregiverSearchCondition();
        condition.setRegionId(regionId);
        condition.setDayOfWeek(dayOfWeek);
        condition.setStartTime(startTime);
        condition.setEndTime(endTime);
        condition.setTagIds(tagIds);
        condition.setMinRating(minRating);
        condition.setHasCertification(hasCertification);
        return searchService.searchCaregivers(condition, pageable);
    }

    @GetMapping("/help-requests")
    public Page<HelpRequestSearchResponse> helpRequests(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long serviceCategoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) com.project.ieum.entity.request.HelpRequestStatus status,
            Pageable pageable) {

        HelpRequestSearchCondition condition = new HelpRequestSearchCondition();
        condition.setRegionId(regionId);
        condition.setServiceCategoryId(serviceCategoryId);
        condition.setFromDate(fromDate);
        condition.setToDate(toDate);
        condition.setStatus(status);
        return searchService.searchHelpRequests(condition, pageable);
    }
}
