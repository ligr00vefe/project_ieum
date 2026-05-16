package com.project.ieum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "regions",
    uniqueConstraints = @UniqueConstraint(name = "uq_regions_code", columnNames = "code"),
    indexes = @Index(name = "idx_regions_sigungu", columnList = "sido,sigungu"))
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Region {

  // 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 지역 코드
  @Column(nullable = false, length = 20)
  private String code;

  // 시/도
  @Column(nullable = false, length = 40)
  private String sido;

  // 시/군/구
  @Column(nullable = false, length = 40)
  private String sigungu;

  // 동/읍/면
  @Column(length = 40)
  private String dong;

  // 위도
  @Column(precision = 9, scale = 6)
  private BigDecimal latitude;

  // 경도
  @Column(precision = 9, scale = 6)
  private BigDecimal longitude;
}
