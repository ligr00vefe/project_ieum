package com.project.ieum.entity.market;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "market_categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_mc_name", columnNames = "name"))  // 카테고리명 중복 방지
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class MarketCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                      // PK, 자동 증가

    @Column(nullable = false, length = 40)
    private String name;                  // 카테고리명 (예: "전자기기", "의류", "가구")

    @Column(length = 255)
    private String description;           // 카테고리 설명 (선택)
}