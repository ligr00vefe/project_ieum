package com.project.ieum.entity.caregiver;

import com.project.ieum.entity.Region;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "caregiver_service_regions")
@IdClass(CaregiverServiceRegionId.class)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class CaregiverServiceRegion {

  // 활동지원사 프로필 (PK,FK)
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "caregiver_id")
  @ToString.Exclude
  private CaregiverProfile caregiver;

  // 지역 (PK,FK)
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "region_id")
  @ToString.Exclude
  private Region region;
}
