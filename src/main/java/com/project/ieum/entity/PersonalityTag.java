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
}
