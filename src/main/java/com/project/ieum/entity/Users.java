package com.project.ieum.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "writer")
// @ManyToOne(fetch = FetchType.LAZY)는
// toString에서 제외할 때 사용하는 세트 어노테이션 속성이다.
public class Users extends BasicEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String email;
  private String password;
  private String name;
  private String phone;
  private Enum role;
  private String profile_image_url;
  private Enum status;

  public void changeName(String name) {this.name = name;}
  public void changePhone(String phone) {this.phone = phone;}
}
