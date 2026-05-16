package com.project.ieum.entity.user;

import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserDisabilityTypeId implements Serializable {
  private Long user;
  private Long disabilityType;
}
