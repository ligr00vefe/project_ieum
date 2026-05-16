package com.project.ieum.entity.caregiver;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.Gender;
import com.project.ieum.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "caregiver_profiles")
@Check(name = "ck_caregiver_avg_rating", constraints = "avg_rating BETWEEN 0 AND 5")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class CaregiverProfile extends BasicEntity {

  // 사용자 ID (PK, FK → users.id 공유)
  @Id
  @Column(name = "user_id")
  private Long userId;

  // 사용자 (공유 PK)
  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
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

  // 프로필 사진 URL
  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  // 한 줄 소개
  @Column(name = "intro_short", length = 120)
  private String introShort;

  // 상세 소개
  @Column(name = "intro_long", columnDefinition = "TEXT")
  private String introLong;

  // 활동 상태
  @Enumerated(EnumType.STRING)
  @Column(name = "availability_status", nullable = false, length = 16)
  @Builder.Default
  private CaregiverAvailabilityStatus availabilityStatus = CaregiverAvailabilityStatus.AVAILABLE;

  // 자격증 보유 여부
  @Column(name = "has_certification", nullable = false)
  @Builder.Default
  private Boolean hasCertification = false;

  // 자격증 종류
  @Column(name = "certification_type", length = 100)
  private String certificationType;

  // 경력
  @Column(name = "experience", columnDefinition = "TEXT")
  private String experience;

  // 평균 평점 (0.00 ~ 5.00)
  @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
  @Builder.Default
  private BigDecimal avgRating = BigDecimal.ZERO;

  // 완료한 도움 요청
  @Column(name = "total_reviews", nullable = false)
  @Builder.Default
  private Integer totalReviews = 0;

  public void applyRating(BigDecimal newAvg, int newCount) {
    this.avgRating = newAvg;
    this.totalReviews = newCount;
  }

  public void changeAvailability(CaregiverAvailabilityStatus next) {
    this.availabilityStatus = next;
  }

  public void updateCertificationInfo(String certificationType, String experience) {
    this.certificationType = certificationType;
    this.experience = experience;
  }
}
