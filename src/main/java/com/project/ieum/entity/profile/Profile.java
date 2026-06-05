package com.project.ieum.entity.profile;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.Gender;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
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

  // 하위 타입이 요구하는 User.role (USER/CAREGIVER) — 서브클래스가 구현
  protected abstract UserRole expectedRole();

  // User.role ↔ 프로필 타입(discriminator) 정합성 가드.
  // 둘이 어긋난 채 persist/update되는 것을 차단(이중 진실원 방지).
  @PrePersist
  @PreUpdate
  private void verifyRoleConsistency() {
    if (user != null && user.getRole() != null && user.getRole() != expectedRole()) {
      throw new IllegalStateException(
          "User.role(" + user.getRole() + ")와 프로필 타입(" + expectedRole() + ")이 불일치합니다");
    }
  }
}
