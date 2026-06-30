package com.project.ieum.service.recommend;

import com.project.ieum.dto.recommend.CaregiverRecommendationResponse;
import com.project.ieum.dto.recommend.RecommendationScoreDetail;
import com.project.ieum.dto.search.CaregiverSearchCondition;
import com.project.ieum.entity.MbtiType;
import com.project.ieum.entity.PersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverPersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestPersonalityTag;
import com.project.ieum.entity.user.UserPreferredMbti;
import com.project.ieum.repository.*;
import com.project.ieum.repository.HelpRequestApplicationRepository;
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
    private final HelpRequestApplicationRepository helpRequestApplicationRepository;

    public List<CaregiverRecommendationResponse> recommendCaregivers(Long helpRequestId, int limit) {
        HelpRequest request = helpRequestRepository.findById(helpRequestId)
                .orElseThrow(() -> new IllegalArgumentException("도움 요청을 찾을 수 없습니다."));

        // 이미 지원한 활동지원사 ID 집합 — 추천 목록에서 제외
        Set<Long> appliedCaregiverIds = helpRequestApplicationRepository
                .findByHelpRequest_IdOrderByCreatedAtDesc(helpRequestId)
                .stream()
                .map(a -> a.getCaregiver().getUserId())
                .collect(Collectors.toSet());

        CaregiverSearchCondition condition = new CaregiverSearchCondition();

        List<CaregiverProfile> candidates = caregiverProfileRepository
                .searchCaregivers(condition, PageRequest.of(0, Math.max(limit * 5, 30)))
                .getContent();

        Set<Long> requestTagIds = helpRequestPersonalityTagRepository.findByHelpRequest_Id(helpRequestId)
                .stream()
                .map(HelpRequestPersonalityTag::getTag)
                .map(PersonalityTag::getId)
                .collect(Collectors.toSet());

        Set<MbtiType> preferredMbtis = request.getRequester().getPreferredMbtis().stream()
                .map(UserPreferredMbti::getMbtiType)
                .collect(Collectors.toSet());

        return candidates.stream()
                .filter(c -> !appliedCaregiverIds.contains(c.getUserId()))
                .map(caregiver -> score(request, requestTagIds, caregiver, preferredMbtis))
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
        Set<MbtiType> preferredMbtis = request.getRequester().getPreferredMbtis().stream()
                .map(UserPreferredMbti::getMbtiType)
                .collect(Collectors.toSet());
        int baseScore = calculateTimeScore(request, caregiver)
                + calculatePersonalityScore(requestTagIds, caregiver)
                + calculateRatingScore(caregiver.getAvgRating())
                + (Boolean.TRUE.equals(caregiver.getHasCertification()) ? 5 : 0);
        if (preferredMbtis.isEmpty()) {
            return Math.min(100, baseScore * 100 / 70);
        }
        int mbtiScore = calculateMbtiScore(caregiver.getMbtiType(), preferredMbtis);
        return Math.min(100, (baseScore + mbtiScore) * 100 / 90);
    }

    private CaregiverRecommendationResponse score(HelpRequest request, Set<Long> requestTagIds, CaregiverProfile caregiver, Set<MbtiType> preferredMbtis) {
        int regionScore = calculateRegionScore(request, caregiver);
        int timeScore = calculateTimeScore(request, caregiver);
        int personalityScore = calculatePersonalityScore(requestTagIds, caregiver);
        int ratingScore = calculateRatingScore(caregiver.getAvgRating());
        int certificationScore = Boolean.TRUE.equals(caregiver.getHasCertification()) ? 5 : 0;
        int baseScore = regionScore + timeScore + personalityScore + ratingScore + certificationScore;

        boolean mbtiEnabled = !preferredMbtis.isEmpty();
        int mbtiScore = mbtiEnabled ? calculateMbtiScore(caregiver.getMbtiType(), preferredMbtis) : 0;
        int totalScore = mbtiEnabled
                ? Math.min(100, (baseScore + mbtiScore) * 100 / 90)
                : Math.min(100, baseScore * 100 / 70);

        return CaregiverRecommendationResponse.builder()
                .caregiverId(caregiver.getUserId())
                .fullName(caregiver.getFullName())
                .introShort(caregiver.getIntroShort())
                .avgRating(caregiver.getAvgRating())
                .totalReviews(caregiver.getTotalReviews())
                .hasCertification(caregiver.getHasCertification())
                .score(totalScore)
                .mbtiType(caregiver.getMbtiType())
                .detail(RecommendationScoreDetail.builder()
                        .regionScore(regionScore)
                        .timeScore(timeScore)
                        .personalityScore(personalityScore)
                        .ratingScore(ratingScore)
                        .certificationScore(certificationScore)
                        .mbtiScore(mbtiScore)
                        .mbtiEnabled(mbtiEnabled)
                        .build())
                .build();
    }

    private int calculateMbtiScore(MbtiType caregiverMbti, Set<MbtiType> preferredMbtis) {
        if (caregiverMbti == null || preferredMbtis == null || preferredMbtis.isEmpty()) {
            return 0;
        }
        return preferredMbtis.contains(caregiverMbti) ? 20 : 0;
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
