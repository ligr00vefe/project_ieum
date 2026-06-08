package com.project.ieum.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_disability_types")
@IdClass(UserDisabilityTypeId.class)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class UserDisabilityType {

  // 이용자 프로필 (PK,FK)
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  @ToString.Exclude
  private UserProfile user;

  // 장애 유형 (PK,FK)
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "disability_type_id")
  @ToString.Exclude
  private DisabilityType disabilityType;
}
