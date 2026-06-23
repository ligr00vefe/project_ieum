package com.project.ieum.service.market;

import com.project.ieum.dto.market.MarketReviewForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.market.MarketChat;
import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.market.MarketPostStatus;
import com.project.ieum.entity.market.MarketReview;
import com.project.ieum.entity.request.ReviewVisibility;
import com.project.ieum.exception.ForbiddenException;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.market.MarketChatRepository;
import com.project.ieum.repository.market.MarketReviewRepository;
import com.project.ieum.service.common.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MarketReviewService {

    private final MarketReviewRepository marketReviewRepository;
    private final MarketChatRepository marketChatRepository;
    private final CurrentUserService currentUserService;

    // ── 후기 작성 ──
    // ReviewService.create()와 동일한 패턴
    public MarketReview create(Long chatId, MarketReviewForm form) {
        User currentUser = currentUserService.getCurrentUser();

        MarketChat chat = marketChatRepository.findWithDetailById(chatId)
                .orElseThrow(() -> new NotFoundException("채팅방을 찾을 수 없습니다."));

        // 구매자만 후기 작성 가능 (판매자 → 구매자 후기는 추후 확장)
        if (!chat.getBuyer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("구매자만 후기를 작성할 수 있습니다.");
        }

        // 거래 완료(SOLD) 상태 게시글인지 확인
        MarketPost post = chat.getPost();
        if (post.getStatus() != MarketPostStatus.SOLD) {
            throw new IllegalStateException("거래가 완료된 후에만 후기를 작성할 수 있습니다.");
        }

        // 중복 후기 방지 (채팅방당 1건)
        if (marketReviewRepository.existsByChat_Id(chatId)) {
            throw new IllegalStateException("이미 후기를 작성했습니다.");
        }

        return marketReviewRepository.save(MarketReview.builder()
                .chat(chat)
                .author(currentUser)           // 구매자
                .target(chat.getSeller())      // 판매자
                .rating(form.getRating())
                .body(form.getBody())
                .visibility(form.getVisibility())
                .build());
    }

    // ── 후기 수정 ──
    public void edit(Long reviewId, MarketReviewForm form) {
        User currentUser = currentUserService.getCurrentUser();
        MarketReview review = marketReviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("후기를 찾을 수 없습니다."));

        if (!review.getAuthor().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("본인의 후기만 수정할 수 있습니다.");
        }

        review.edit(form.getRating(), form.getBody());
    }

    // ── 특정 판매자의 공개 후기 목록 ──
    // 판매자 프로필 페이지에서 사용
    @Transactional(readOnly = true)
    public List<MarketReview> getPublicReviews(Long sellerUserId) {
        return marketReviewRepository.findByTargetWithFetch(sellerUserId, ReviewVisibility.PUBLIC);
    }

    // ── 판매자의 마켓 평균 별점 ──
    @Transactional(readOnly = true)
    public double getAverageRating(Long sellerUserId) {
        return marketReviewRepository.averageRatingByTargetUserId(sellerUserId);
    }

    // ── 판매자의 매너온도 ──
    // 기본 36.5°C, 별점별 누적: 5점=+3, 4점=+1.5, 3점=0, 2점=-1.5, 1점=-3. 범위 0~99.
    @Transactional(readOnly = true)
    public double getMannerTemperature(Long sellerUserId) {
        Double raw = marketReviewRepository.mannerTemperatureByTargetUserId(sellerUserId);
        double temp = (raw != null) ? raw : 36.5;
        return Math.min(99.0, Math.max(0.0, temp));
    }
}