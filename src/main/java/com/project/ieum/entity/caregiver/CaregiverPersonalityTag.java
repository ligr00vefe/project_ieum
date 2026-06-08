package com.project.ieum.entity.caregiver;

import com.project.ieum.entity.PersonalityTag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "caregiver_personality_tags")
@IdClass(CaregiverPersonalityTagId.class)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class CaregiverPersonalityTag {

  // 활동지원사 프로필 (PK,FK)
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "caregiver_id")
  @ToString.Exclude
  private CaregiverProfile caregiver;

  // 성향 태그 (PK,FK)
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tag_id")
  @ToString.Exclude
  private PersonalityTag tag;
}
