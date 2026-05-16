package com.project.ieum.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "disability_types",
    uniqueConstraints = @UniqueConstraint(name = "uq_dt_code", columnNames = "code"))
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class DisabilityType {

  // 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 유형 코드
  @Column(nullable = false, length = 24)
  private String code;

  // 유형명
  @Column(name = "name_ko", nullable = false, length = 80)
  private String nameKo;

  // 정렬 순서
  @Column(name = "sort_order", nullable = false)
  private Short sortOrder;
}
