package com.project.ieum.service.recommend;

import com.project.ieum.dto.recommend.CaregiverRecommendationResponse;
import com.project.ieum.dto.recommend.RecommendationScoreDetail;
import com.project.ieum.dto.search.CaregiverSearchCondition;
import com.project.ieum.entity.PersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverPersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestPersonalityTag;
import com.project.ieum.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final HelpRequestRepository helpRequestRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final CaregiverAvailabilityRepository caregiverAvailabilityRepository;
    private final CaregiverPersonalityTagRepository caregiverPersonalityTagRepository;
    private final HelpRequestPersonalityTagRepository helpRequestPersonalityTagRepository;

    public List<CaregiverRecommendationResponse> recommendCaregivers(Long helpRequestId, int limit) {
        HelpRequest request = helpRequestRepository.findById(helpRequestId)
                .orElseThrow(() -> new IllegalArgumentException("도움 요청을 찾을 수 없습니다."));

        CaregiverSearchCondition condition = new CaregiverSearchCondition();
        if (request.getDesiredStartDatetime() != null) {
            condition.setDayOfWeek((short) (request.getDesiredStartDatetime().getDayOfWeek().getValue() % 7));
            condition.setStartTime(request.getDesiredStartDatetime().toLocalTime());
            if (request.getDesiredEndDatetime() != null) {
                condition.setEndTime(request.getDesiredEndDatetime().toLocalTime());
            }
        }

        List<CaregiverProfile> candidates = caregiverProfileRepository
                .searchCaregivers(condition, PageRequest.of(0, Math.max(limit * 5, 30)))
                .getContent();

        Set<Long> requestTagIds = helpRequestPersonalityTagRepository.findByHelpRequest_Id(helpRequestId)
                .stream()
                .map(HelpRequestPersonalityTag::getTag)
                .map(PersonalityTag::getId)
                .collect(Collectors.toSet());

        return candidates.stream()
                .map(caregiver -> score(request, requestTagIds, caregiver))
                .sorted(Comparator.comparingInt(CaregiverRecommendationResponse::getScore).reversed())
                .limit(limit)
                .toList();
    }

    public int scorePercent(Long requestId, CaregiverProfile caregiver) {
        HelpRequest request = helpRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("도움 요청을 찾을 수 없습니다."));
        Set<Long> requestTagIds = helpRequestPersonalityTagRepository.findByHelpRequest_Id(requestId)
                .stream()
                .map(t -> t.getTag().getId())
                .collect(Collectors.toSet());
        int total = calculateTimeScore(request, caregiver)
                + calculatePersonalityScore(requestTagIds, caregiver)
                + calculateRatingScore(caregiver.getAvgRating())
                + (Boolean.TRUE.equals(caregiver.getHasCertification()) ? 5 : 0);
        return Math.min(100, total * 100 / 70);
    }

    private CaregiverRecommendationResponse score(HelpRequest request, Set<Long> requestTagIds, CaregiverProfile caregiver) {
        int regionScore = calculateRegionScore(request, caregiver);
        int timeScore = calculateTimeScore(request, caregiver);
        int personalityScore = calculatePersonalityScore(requestTagIds, caregiver);
        int ratingScore = calculateRatingScore(caregiver.getAvgRating());
        int certificationScore = Boolean.TRUE.equals(caregiver.getHasCertification()) ? 5 : 0;
        int totalScore = regionScore + timeScore + personalityScore + ratingScore + certificationScore;

        return CaregiverRecommendationResponse.builder()
                .caregiverId(caregiver.getUserId())
                .fullName(caregiver.getFullName())
                .introShort(caregiver.getIntroShort())
                .avgRating(caregiver.getAvgRating())
                .totalReviews(caregiver.getTotalReviews())
                .hasCertification(caregiver.getHasCertification())
                .score(totalScore)
                .detail(RecommendationScoreDetail.builder()
                        .regionScore(regionScore)
                        .timeScore(timeScore)
                        .personalityScore(personalityScore)
                        .ratingScore(ratingScore)
                        .certificationScore(certificationScore)
                        .build())
                .build();
    }

    // 서비스권역 정규화(#11) 이후 지역 매칭은 전역 검색으로 전환됨
    // TODO(region-score): 근거리 정렬(위/경도 거리)로 대체 예정
    private int calculateRegionScore(HelpRequest request, CaregiverProfile caregiver) {
        return 0;
    }

    private int calculateTimeScore(HelpRequest request, CaregiverProfile caregiver) {
        if (request.getDesiredStartDatetime() == null) {
            return 10;
        }

        Short day = (short) (request.getDesiredStartDatetime().getDayOfWeek().getValue() % 7);
        LocalTime start = request.getDesiredStartDatetime().toLocalTime();
        LocalTime end = request.getDesiredEndDatetime() != null
                ? request.getDesiredEndDatetime().toLocalTime()
                : null;

        if (end == null) {
            return 10;
        }

        boolean available = caregiverAvailabilityRepository.findByCaregiver_UserId(caregiver.getUserId()).stream()
                .anyMatch(time -> time.getDayOfWeek().equals(day)
                        && !time.getStartTime().isAfter(start)
                        && !time.getEndTime().isBefore(end));
        return available ? 25 : 0;
    }

    private int calculatePersonalityScore(Set<Long> requestTagIds, CaregiverProfile caregiver) {
        if (requestTagIds.isEmpty()) {
            return 0;
        }

        Set<Long> caregiverTagIds = caregiverPersonalityTagRepository.findByCaregiver(caregiver).stream()
                .map(CaregiverPersonalityTag::getTag)
                .map(PersonalityTag::getId)
                .collect(Collectors.toSet());

        long matched = requestTagIds.stream()
                .filter(caregiverTagIds::contains)
                .count();

        return (int) Math.min(matched * 10, 20);
    }

    private int calculateRatingScore(BigDecimal avgRating) {
        if (avgRating == null) {
            return 0;
        }
        return avgRating.multiply(BigDecimal.valueOf(4)).intValue();
    }
}
