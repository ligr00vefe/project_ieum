package com.project.ieum.entity.user;

import lombok.*;

import java.io.Serializable;

// @IdClass 복합키 — 엔티티의 @Id 연관필드명(user, communicationMethod)과 동일,
// 타입은 각 연관의 PK 타입(Long). 다른 조인 엔티티(@IdClass)와 매핑 전략 통일.
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserCommunicationMethodId implements Serializable {
  private Long user;
  private Long communicationMethod;
}
