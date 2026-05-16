package com.project.ieum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uq_users_phone", columnNames = "phone")
    })
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class User extends BasicEntity {

  // 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 이메일
  @Column(nullable = false, length = 255)
  private String email;

  // 전화번호
  @Column(length = 20)
  private String phone;

  // 비밀번호 해시
  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  // 역할 (이용자/활동지원사/관리자)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private UserRole role;

  // 상태 (활성/일시중지/정지/탈퇴)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private UserStatus status;

  // 최근 로그인 시각
  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  // 삭제 시각 (soft delete)
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void changeEmail(String email) { this.email = email; }
  public void changePhone(String phone) { this.phone = phone; }
  public void markDeleted() { this.deletedAt = LocalDateTime.now(); }
}
