package com.project.ieum.entity.profile;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.Gender;
import com.project.ieum.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

// User 1명은 UserProfile 또는 CaregiverProfile 중 정확히 하나만 가질 수 있도록
// JOINED 상속 + Discriminator로 상호 배타성을 DB 스키마 레벨에서 보장한다.
@Entity
@Table(name = "profiles")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "profile_type", length = 16)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public abstract class Profile extends BasicEntity {

  // 사용자 ID (PK, FK → users.id 공유)
  @Id
  @Column(name = "user_id")
  private Long userId;

  // 사용자 (공유 PK)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  @ToString.Exclude
  private User user;

  // 이름
  @Column(name = "full_name", nullable = false, length = 80)
  private String fullName;

  // 생년월일
  @Column(name = "birth_date")
  private LocalDate birthDate;

  // 성별
  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private Gender gender;
}
