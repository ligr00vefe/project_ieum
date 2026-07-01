package com.project.ieum.entity.user;

import com.project.ieum.entity.MbtiType;
import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserPreferredMbtiId implements Serializable {
  private Long userProfile;
  private MbtiType mbtiType;
}
