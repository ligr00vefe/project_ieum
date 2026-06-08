package com.project.ieum.entity.caregiver;

import lombok.*;

import java.io.Serializable;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CaregiverAvailabilityId implements Serializable {
  private Long caregiver;
  private Short dayOfWeek;
  private LocalTime startTime;
}
