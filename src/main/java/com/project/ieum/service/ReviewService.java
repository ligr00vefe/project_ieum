package com.project.ieum.service;

import com.project.ieum.dto.request.ReviewForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.notification.NotificationType;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.Review;
import com.project.ieum.entity.request.ReviewVisibility;
import com.project.ieum.exception.ForbiddenException;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.CaregiverProfileRepository;
import com.project.ieum.repository.HelpRequestRepository;
import com.project.ieum.repository.ReviewRepository;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final MatchingService matchingService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public Review create(Long requestId, ReviewForm form) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != UserRole.USER) {
            throw new ForbiddenException("이용자만 후기를 작성할 수 있습니다.");
        }

        HelpRequest helpRequest = helpRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("도움 요청을 찾을 수 없습니다."));
        if (!helpRequest.getRequester().getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("본인의 요청에만 후기를 작성할 수 있습니다.");
        }
        if (helpRequest.getStatus() != HelpRequestStatus.COMPLETED) {
            throw new IllegalStateException("완료된 활동에만 후기를 작성할 수 있습니다.");
        }
        if (reviewRepository.existsByHelpRequest_Id(requestId)) {
            throw new IllegalStateException("이미 후기를 작성했습니다.");
        }

        HelpRequestApplication acceptedApplication = matchingService.findAcceptedApplication(requestId);
        CaregiverProfile caregiver = acceptedApplication.getCaregiver();
        Review review = reviewRepository.save(Review.builder()
                .helpRequest(helpRequest)
                .author(helpRequest.getRequester())
                .target(caregiver)
                .rating(form.getRating())
                .body(form.getBody())
                .visibility(form.getVisibility())
                .build());
        refreshRating(caregiver.getUserId());
        notificationService.create(
                caregiver.getUser(),
                NotificationType.REVIEW,
                "새 후기가 등록되었어요",
                "받은 활동에 대한 후기가 작성되었습니다.",
                "/caregiver/mypage"
        );
        return review;
    }

    @Transactional(readOnly = true)
    public List<Review> getPublicReviews(Long caregiverUserId) {
        return reviewRepository.findByTargetWithFetch(caregiverUserId, ReviewVisibility.PUBLIC);
    }

    @Transactional(readOnly = true)
    public long countWrittenByUserId(Long userId) {
        return reviewRepository.countByAuthor_UserId(userId);
    }

    private void refreshRating(Long caregiverUserId) {
        CaregiverProfile caregiver = caregiverProfileRepository.findById(caregiverUserId)
                .orElseThrow(() -> new NotFoundException("활동지원사 프로필을 찾을 수 없습니다."));
        int count = reviewRepository.countByTarget_UserId(caregiverUserId);
        Double average = reviewRepository.averageRatingByTargetUserId(caregiverUserId);
        BigDecimal avgRating = BigDecimal.valueOf(average == null ? 0 : average)
                .setScale(2, RoundingMode.HALF_UP);
        caregiver.applyRating(avgRating, count);
    }
}
