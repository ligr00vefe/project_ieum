package com.project.ieum.entity.popup;

import com.project.ieum.entity.BasicEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "popups", indexes = @Index(name = "idx_popup_enabled_expires", columnList = "enabled, expires_at"))
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Popup extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(length = 20)
    private String duration;

    @Column(length = 20)
    private String layout;

    @Column(length = 500)
    private String linkUrl;

    public void update(String name, String content, LocalDateTime expiresAt, String duration, String layout, String linkUrl) {
        this.name = name;
        this.content = content;
        this.expiresAt = expiresAt;
        this.duration = duration;
        this.layout = layout;
        this.linkUrl = linkUrl;
    }

    public void toggleEnabled() {
        this.enabled = !this.enabled;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
