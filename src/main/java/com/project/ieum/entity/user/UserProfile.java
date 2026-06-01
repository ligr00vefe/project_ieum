package com.project.ieum.entity.user;

import com.project.ieum.entity.Region;
import com.project.ieum.entity.profile.Profile;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_profiles")
@DiscriminatorValue("USER")
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(callSuper = true)
public class UserProfile extends Profile {

  // 보호자 이름
  @Column(name = "guardian_name", nullable = false, length = 80)
  private String guardianName;

  // 보호자 연락처
  @Column(name = "guardian_phone", nullable = false, length = 20)
  private String guardianPhone;

  // 지역
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "region_id")
  @ToString.Exclude
  private Region region;

  // 상세주소
  @Column(name = "address_detail", length = 255)
  private String addressDetail;

  // 이동 보조 기구
  @Column(name = "mobility_aid", length = 40)
  private String mobilityAid;

  // 활동 가능 범위
  @Column(name = "activity_range", columnDefinition = "TEXT")
  private String activityRange;

  // 피해야 할 상황
  @Column(name = "avoid_situations", columnDefinition = "TEXT")
  private String avoidSituations;

  // 생활 메모
  @Column(name = "lifestyle_note", columnDefinition = "TEXT")
  private String lifestyleNote;

  // 자기소개
  @Column(name = "intro_text", columnDefinition = "TEXT")
  private String introText;

  // 보유 장애 유형 (다대다)
  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @Builder.Default
  private Set<UserDisabilityType> disabilityTypes = new HashSet<>();

  // 의사소통 방식 (다대다)
  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @Builder.Default
  private Set<UserCommunicationMethod> communicationMethods = new HashSet<>();

  public void addDisabilityType(DisabilityType type) {
    disabilityTypes.add(UserDisabilityType.builder().user(this).disabilityType(type).build());
  }

  public void removeDisabilityType(DisabilityType type) {
    disabilityTypes.removeIf(udt -> udt.getDisabilityType().getId().equals(type.getId()));
  }

  public void addCommunicationMethod(com.project.ieum.entity.CommunicationMethod method) {
    communicationMethods.add(UserCommunicationMethod.builder()
        .id(new UserCommunicationMethodId(getUserId(), method.getId()))
        .user(this)
        .communicationMethod(method)
        .build());
  }

  public void removeCommunicationMethod(com.project.ieum.entity.CommunicationMethod method) {
    communicationMethods.removeIf(ucm -> ucm.getCommunicationMethod().getId().equals(method.getId()));
  }

  public void updateGuardian(String name, String phone) {
    this.guardianName = name;
    this.guardianPhone = phone;
  }

  public void updateRegion(Region region, String addressDetail) {
    this.region = region;
    this.addressDetail = addressDetail;
  }

  public void updateIntro(String introText) {
    this.introText = introText;
  }

  public void updateActivityInfo(String activityRange, String avoidSituations) {
    this.activityRange = activityRange;
    this.avoidSituations = avoidSituations;
  }
}
