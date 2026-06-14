package com.project.ieum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "personality_tags",
    uniqueConstraints = @UniqueConstraint(name = "uq_pt_name", columnNames = "name"))
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class PersonalityTag {

  // 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 태그명
  @Column(name = "name", nullable = false, length = 40)
  private String name;

  // 활동지원사 전용 여부 (true: 활동지원사 성향에만 노출, false: 장애인/공통)
  @Column(name = "is_caregivers_only", nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 0")
  @Builder.Default
  private Boolean isCaregiversOnly = false;
}
