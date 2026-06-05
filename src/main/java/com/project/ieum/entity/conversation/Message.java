package com.project.ieum.entity.conversation;

import com.project.ieum.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages",
    indexes = {
        @Index(name = "idx_msg_conv_sent", columnList = "conversation_id,sent_at"),
        @Index(name = "idx_msg_sender", columnList = "sender_id")
    })
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Message {

  // 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 대화방
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id", nullable = false)
  @ToString.Exclude
  private Conversation conversation;

  // 보낸이
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_id", nullable = false)
  @ToString.Exclude
  private User sender;

  // 본문
  @Column(columnDefinition = "TEXT")
  private String body;

  // 첨부 URL
  @Column(name = "attachment_url", length = 500)
  private String attachmentUrl;

  // 첨부 유형
  @Column(name = "attachment_type", length = 40)
  private String attachmentType;

  // 읽음 여부
  @Column(name = "has_read", nullable = false)
  @Builder.Default
  private Boolean hasRead = false;

  // 보낸 시각 (Hibernate 자동 주입)
  @CreationTimestamp
  @Column(name = "sent_at", nullable = false, updatable = false)
  private LocalDateTime sentAt;

  public void markRead() { this.hasRead = true; }
}
