package com.project.ieum.entity.request;

import com.project.ieum.entity.PersonalityTag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "help_request_personality_tags")
@IdClass(HelpRequestPersonalityTagId.class)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class HelpRequestPersonalityTag {

  // 도움요청 (PK,FK)
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "help_request_id")
  @ToString.Exclude
  private HelpRequest helpRequest;

  // 성향 태그 (PK,FK)
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tag_id")
  @ToString.Exclude
  private PersonalityTag tag;
}
