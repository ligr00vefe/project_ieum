package com.project.ieum.entity.market;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "market_posts",
        indexes = {
                // 상태 + 생성일 복합 인덱스: "판매중 목록 최신순" 쿼리 성능
                @Index(name = "idx_mp_status_created", columnList = "status,created_at"),
                // 판매자별 "내 게시글" 조회 성능
                @Index(name = "idx_mp_seller", columnList = "seller_id,status")
        })
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class MarketPost extends BasicEntity {  // BasicEntity 상속 → created_at, updated_at 자동 관리

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 판매자 — User 직접 참조 (UserProfile이 아닌 이유: USER/CAREGIVER 구분 없이 누구나 판매 가능)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    @ToString.Exclude
    private User seller;

    // 카테고리 (전자기기, 의류 등)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    private MarketCategory category;

    // 상품명
    @Column(nullable = false, length = 120)
    private String title;

    // 상품 설명
    @Column(columnDefinition = "TEXT")
    private String description;

    // 판매 희망가 (원 단위)
    // BigDecimal 사용 이유: float/double의 부동소수점 오차 없이 금액을 정확하게 저장
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    // ── 만남 장소 스냅샷 (HelpRequest와 완전히 동일한 write-once 설계) ──
    // 카카오 주소검색 API  → roadAddress, sido, sigungu, bname, zonecode 채움
    // TmapGeocodingService → latitude, longitude 채움 (기존 서비스 재사용)

    @Column(name = "road_address", nullable = false, length = 255)
    private String roadAddress;            // 도로명주소 (필수)

    @Column(name = "address_detail", length = 255)
    private String addressDetail;          // 상세주소 (선택, 예: "2번 출구 앞")

    @Column(name = "sido", nullable = false, length = 20)
    private String sido;                   // 시/도 (예: "서울특별시")

    @Column(name = "sigungu", nullable = false, length = 40)
    private String sigungu;               // 시/군/구 (예: "강남구")

    @Column(name = "bname", length = 40)
    private String bname;                  // 법정동 (예: "역삼동")

    @Column(name = "zonecode", length = 5)
    private String zonecode;              // 우편번호

    // 위도/경도 — "내 근처 매물" 거리순 정렬에 사용 (HelpRequest와 동일한 precision/scale)
    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    // 게시글 상태 — 기본값 ACTIVE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private MarketPostStatus status = MarketPostStatus.ACTIVE;

    // 나눔 여부 — true이면 무료 나눔, false이면 판매
    @Column(nullable = false)
    @Builder.Default
    private boolean sharing = false;

    // ── 상태 전이 메서드 (HelpRequest 패턴 동일하게 적용) ──

    // 예약 처리
    public void reserve() {
        requireStatus(MarketPostStatus.ACTIVE, "예약은 판매중(ACTIVE) 상태에서만 가능합니다.");
        this.status = MarketPostStatus.RESERVED;
    }

    // 예약 취소 → 다시 판매중으로
    public void cancelReservation() {
        requireStatus(MarketPostStatus.RESERVED, "예약 취소는 예약중(RESERVED) 상태에서만 가능합니다.");
        this.status = MarketPostStatus.ACTIVE;
    }

    // 거래 완료 (양쪽 확정 시 Service에서 호출)
    public void complete() {
        if (status != MarketPostStatus.ACTIVE && status != MarketPostStatus.RESERVED) {
            throw new IllegalStateException("거래 완료는 판매중 또는 예약중 상태에서만 가능합니다.");
        }
        this.status = MarketPostStatus.SOLD;
    }

    // 게시글 삭제 (soft delete — REMOVED 상태로 전환)
    public void remove() {
        if (status == MarketPostStatus.SOLD) {
            throw new IllegalStateException("판매완료된 게시글은 삭제할 수 없습니다.");
        }
        this.status = MarketPostStatus.REMOVED;
    }

    // 상품 정보 수정 — ACTIVE 상태에서만 허용 (Service에서 상태 검증 후 호출)
    public void update(String title, String description, BigDecimal price, boolean sharing) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.sharing = sharing;
    }

    // 내부 상태 가드 — HelpRequest.requireStatus와 동일한 패턴
    private void requireStatus(MarketPostStatus expected, String message) {
        if (this.status != expected) {
            throw new IllegalStateException(message);
        }
    }
}