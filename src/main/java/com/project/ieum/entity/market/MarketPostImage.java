package com.project.ieum.entity.market;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_post_images",
        indexes = {
                // post_id + display_order 복합 인덱스: 슬라이드 순서대로 이미지 조회 성능
                @Index(name = "idx_mpi_post_order", columnList = "post_id,display_order")
        })
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class MarketPostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 게시글의 이미지인지 (N:1)
    // CascadeType 없음 — 이미지 저장/삭제는 MarketPostService에서 명시적으로 처리
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private MarketPost post;

    // 저장된 이미지 URL (예: "/uploads/market/1720000000000_상품.jpg")
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    // 표시 순서 (0~4, 0번이 대표 이미지 — 목록 썸네일에 사용)
    // 5장 초과 검증은 MarketPostService.create()에서 처리
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}