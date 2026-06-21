package com.project.ieum.entity.auth;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 토큰. 고엔트로피 1회용 토큰을 단시간(예: 30분) 만료로 발급한다.
 * 검증 통과 시 비밀번호를 BCrypt로 재인코딩하고 토큰을 즉시 사용 처리(used=true)한다.
 */
@Entity
@Table(name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_prt_token", columnList = "token"),
                @Index(name = "idx_prt_user", columnList = "user_id")
        })
@Getter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class PasswordResetToken extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    public void markUsed() {
        this.used = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** 미사용 && 미만료일 때만 재설정에 사용할 수 있다. */
    public boolean isUsable() {
        return !used && !isExpired();
    }
}
