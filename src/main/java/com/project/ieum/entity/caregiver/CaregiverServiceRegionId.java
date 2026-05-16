package com.project.ieum.entity.caregiver;

import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CaregiverServiceRegionId implements Serializable {
  private Long caregiver;
  private Long region;
}
