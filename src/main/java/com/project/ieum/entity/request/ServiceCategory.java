package com.project.ieum.entity.request;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_categories",
    uniqueConstraints = @UniqueConstraint(name = "uq_sc_code", columnNames = "code"))
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class ServiceCategory {

  // 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 분류 코드
  @Column(nullable = false, length = 24)
  private String code;

  // 분류명
  @Column(name = "name_ko", nullable = false, length = 40)
  private String nameKo;

  // 설명
  @Column(length = 255)
  private String description;
}
