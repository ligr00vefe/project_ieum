package com.project.ieum.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
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
