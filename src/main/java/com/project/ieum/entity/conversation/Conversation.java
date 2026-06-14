package com.project.ieum.entity.conversation;

import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.user.UserProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations",
    uniqueConstraints = @UniqueConstraint(name = "uq_conv_application", columnNames = "application_id"),
    indexes = {
        @Index(name = "idx_conv_requester", columnList = "requester_id,last_message_at"),
        @Index(name = "idx_conv_caregiver", columnList = "caregiver_id,last_message_at")
    })
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Conversation {

  // 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 지원 신청 (지원 시점에 1:1 자동 생성)
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false)
  @ToString.Exclude
  private HelpRequestApplication application;

  // 요청자 (이용자 프로필)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "requester_id", nullable = false)
  @ToString.Exclude
  private UserProfile requester;

  // 활동지원사 (지원사 프로필)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "caregiver_id", nullable = false)
  @ToString.Exclude
  private CaregiverProfile caregiver;

  // 상태
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ConversationStatus status;

  // 최근 메시지 시각
  @Column(name = "last_message_at")
  private LocalDateTime lastMessageAt;

  // 생성 시각 (Hibernate 자동 주입)
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public void touchLastMessage() { this.lastMessageAt = LocalDateTime.now(); }
  public void close() { this.status = ConversationStatus.CLOSED; }
  public void open() { this.status = ConversationStatus.ACTIVE; }
}
