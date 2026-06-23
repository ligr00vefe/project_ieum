package com.project.ieum.dto.market;

import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.market.MarketPostStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class MarketPostResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;

    private String categoryName;      // 카테고리명 (템플릿에서 category.name 대신 사용)

    private MarketPostStatus status;  // 상태값 (템플릿에서 뱃지 표시용)
    private String statusLabel;       // 상태 한글 라벨 (예: "판매중", "예약중", "판매완료")

    // 위치 정보
    private String roadAddress;
    private String addressDetail;
    private String sido;
    private String sigungu;

    // 판매자 정보
    private Long sellerId;
    private String sellerEmail;       // 판매자 식별용 (이름 대신 이메일 — User 엔티티 기준)

    // 대표 이미지 URL (목록 썸네일용 — display_order=0 이미지)
    // 이미지가 없으면 null
    private String thumbnailUrl;

    // 날짜
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 관심 구매자 수 (채팅방 개수)
    private int chatCount;

    // ── 정적 팩토리 메서드 ──
    // Service/Controller에서 MarketPost → MarketPostResponse 변환 시 사용
    public static MarketPostResponse from(MarketPost post, String thumbnailUrl, int chatCount) {
        return MarketPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .description(post.getDescription())
                .price(post.getPrice())
                .categoryName(post.getCategory().getName())
                .status(post.getStatus())
                .statusLabel(toStatusLabel(post.getStatus()))
                .roadAddress(post.getRoadAddress())
                .addressDetail(post.getAddressDetail())
                .sido(post.getSido())
                .sigungu(post.getSigungu())
                .sellerId(post.getSeller().getId())
                .sellerEmail(post.getSeller().getEmail())
                .thumbnailUrl(thumbnailUrl)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .chatCount(chatCount)
                .build();
    }

    // 채팅 수 없는 간단 변환 (목록용)
    public static MarketPostResponse from(MarketPost post, String thumbnailUrl) {
        return from(post, thumbnailUrl, 0);
    }

    // 상태 → 한글 라벨 변환
    private static String toStatusLabel(MarketPostStatus status) {
        return switch (status) {
            case ACTIVE   -> "판매중";
            case RESERVED -> "예약중";
            case SOLD     -> "판매완료";
            case REMOVED  -> "삭제됨";
        };
    }
}