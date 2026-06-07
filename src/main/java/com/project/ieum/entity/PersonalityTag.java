package com.project.ieum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "personality_tags",
    uniqueConstraints = @UniqueConstraint(name = "uq_pt_name", columnNames = "name_ko"))
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
  @Column(name = "name_ko", nullable = false, length = 40)
  private String nameKo;
}
