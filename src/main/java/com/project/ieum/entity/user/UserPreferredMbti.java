package com.project.ieum.entity.user;

import com.project.ieum.entity.MbtiType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_preferred_mbti")
@IdClass(UserPreferredMbtiId.class)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class UserPreferredMbti {

  // 이용자 프로필 (PK, FK)
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  @ToString.Exclude
  private UserProfile userProfile;

  // 선호 MBTI 유형 (PK)
  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "mbti_type", length = 8)
  private MbtiType mbtiType;
}
