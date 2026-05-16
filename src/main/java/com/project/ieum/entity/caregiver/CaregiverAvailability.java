package com.project.ieum.entity.caregiver;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "caregiver_availability")
@IdClass(CaregiverAvailabilityId.class)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class CaregiverAvailability {

  // 활동지원사 프로필 (PK,FK)
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "caregiver_id")
  @ToString.Exclude
  private CaregiverProfile caregiver;

  // 요일 (0~6, PK)
  @Id
  @Column(name = "day_of_week")
  private Short dayOfWeek;

  // 시작 시각 (PK)
  @Id
  @Column(name = "start_time")
  private LocalTime startTime;

  // 종료 시각
  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;
}
