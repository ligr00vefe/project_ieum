package com.project.ieum.entity.request;

import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HelpRequestPersonalityTagId implements Serializable {
  private Long helpRequest;
  private Long tag;
}
