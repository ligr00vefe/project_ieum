package com.project.ieum.entity.market;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.User;
import com.project.ieum.entity.request.ReviewVisibility;  // 기존 enum 재사용 (PUBLIC/PRIVATE)
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "market_reviews",
        uniqueConstraints = @UniqueConstraint(
                // 채팅방 1개당 후기 1건만 허용
                name = "uq_mr_chat", columnNames = "chat_id"),
        indexes = @Index(name = "idx_mr_target", columnList = "target_id,created_at"))
@Check(name = "ck_mr_rating", constraints = "rating BETWEEN 1 AND 5")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class MarketReview extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 거래 채팅방에 대한 후기인지 (1:1)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    @ToString.Exclude
    private MarketChat chat;

    // 후기 작성자 (구매자)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @ToString.Exclude
    private User author;

    // 후기 대상 (판매자) — User 직접 참조로 매칭 별점과 완전 분리
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    @ToString.Exclude
    private User target;

    // 별점 1~5 — DB 제약(@Check) + 애플리케이션 검증(@Min/@Max) 이중 방어
    @Min(1) @Max(5)
    @Column(nullable = false)
    private Short rating;

    // 후기 본문 (선택)
    @Column(columnDefinition = "TEXT")
    private String body;

    // 공개 여부 — 기존 ReviewVisibility enum 재사용 (PUBLIC / PRIVATE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReviewVisibility visibility;

    // 수정 메서드 — Review.java와 완전히 동일한 패턴
    public void edit(Short rating, String body) {
        this.rating = rating;
        this.body = body;
    }

    public void changeVisibility(ReviewVisibility visibility) {
        this.visibility = visibility;
    }
}