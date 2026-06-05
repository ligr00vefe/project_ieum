package com.project.ieum.entity.caregiver;

import com.project.ieum.entity.profile.Profile;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(name = "caregiver_profiles")
@DiscriminatorValue("CAREGIVER")
@Check(name = "ck_caregiver_avg_rating", constraints = "avg_rating BETWEEN 0 AND 5")
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class CaregiverProfile extends Profile {

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

  // 가능 업무 (쉼표 구분 텍스트: "이동 보조,병원 동행,...")
  @Column(name = "service_categories", columnDefinition = "TEXT")
  private String serviceCategories;

  // 가능 시간대 (쉼표 구분 텍스트: "오전 (09-12),주말,...")
  @Column(name = "available_time_slots", columnDefinition = "TEXT")
  private String availableTimeSlots;

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
