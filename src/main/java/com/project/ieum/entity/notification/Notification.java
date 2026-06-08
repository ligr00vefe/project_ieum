package com.project.ieum.entity.notification;

import com.project.ieum.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notification_receiver_read", columnList = "receiver_id,has_read,created_at"),
                @Index(name = "idx_notification_receiver", columnList = "receiver_id,created_at")
        })
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    @ToString.Exclude
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationType type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(length = 500)
    private String url;

    @Column(name = "has_read", nullable = false)
    @Builder.Default
    private Boolean hasRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markRead() {
        this.hasRead = true;
    }
}
