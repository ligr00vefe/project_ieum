package com.project.ieum.service.auth;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.auth.PasswordResetToken;
import com.project.ieum.repository.PasswordResetTokenRepository;
import com.project.ieum.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PasswordResetService} 단위 검증 — 이메일 열거 방지·토큰 1회용·만료·BCrypt 재인코딩 가드(영속성은 mock).
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    @DisplayName("가입 계정이면 토큰을 발급하고 재설정 경로를 반환한다")
    void requestReset_existingUser_savesToken() {
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(activeUser(1L)));

        Optional<String> path = passwordResetService.requestReset("user@email.com");

        verify(tokenRepository).save(any(PasswordResetToken.class));
        assertThat(path).isPresent();
        assertThat(path.get()).startsWith("/password/reset?token=");
    }

    @Test
    @DisplayName("미가입 이메일은 토큰을 저장하지 않고 빈 결과를 반환한다(열거 방지)")
    void requestReset_unknownEmail_doesNotSave() {
        when(userRepository.findByEmail("none@email.com")).thenReturn(Optional.empty());

        Optional<String> path = passwordResetService.requestReset("none@email.com");

        assertThat(path).isEmpty();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("탈퇴(DELETED) 계정은 토큰을 저장하지 않는다")
    void requestReset_deletedUser_doesNotSave() {
        User deleted = User.builder().id(2L).email("user@email.com")
                .passwordHash("old").status(UserStatus.DELETED).build();
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(deleted));

        Optional<String> path = passwordResetService.requestReset("user@email.com");

        assertThat(path).isEmpty();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("유효한 토큰이면 비밀번호를 재인코딩하고 토큰을 사용 처리한다")
    void resetPassword_validToken_changesPasswordAndConsumesToken() {
        User user = activeUser(1L);
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user).token("valid")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        when(tokenRepository.findByToken("valid")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword1")).thenReturn("ENCODED");

        passwordResetService.resetPassword("valid", "newPassword1");

        assertThat(user.getPasswordHash()).isEqualTo("ENCODED");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("이미 사용된 토큰은 거부한다(1회용)")
    void resetPassword_usedToken_throws() {
        User user = activeUser(1L);
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user).token("used")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        token.markUsed();
        when(tokenRepository.findByToken("used")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("used", "newPassword1"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("만료된 토큰은 거부한다")
    void resetPassword_expiredToken_throws() {
        PasswordResetToken token = PasswordResetToken.builder()
                .user(activeUser(1L)).token("expired")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired", "newPassword1"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 토큰은 거부한다")
    void resetPassword_unknownToken_throws() {
        when(tokenRepository.findByToken("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("ghost", "newPassword1"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("isValidToken은 미사용·미만료 토큰에만 true")
    void isValidToken_reflectsUsability() {
        PasswordResetToken usable = PasswordResetToken.builder()
                .user(activeUser(1L)).token("ok")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        when(tokenRepository.findByToken("ok")).thenReturn(Optional.of(usable));
        when(tokenRepository.findByToken("ghost")).thenReturn(Optional.empty());

        assertThat(passwordResetService.isValidToken("ok")).isTrue();
        assertThat(passwordResetService.isValidToken("ghost")).isFalse();
    }

    private User activeUser(Long id) {
        return User.builder().id(id).email("user@email.com")
                .passwordHash("oldHash").status(UserStatus.ACTIVE).build();
    }
}
