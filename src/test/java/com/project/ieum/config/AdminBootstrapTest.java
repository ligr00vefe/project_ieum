package com.project.ieum.config;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AdminBootstrap} 단위 검증.
 *
 * <p>이 클래스가 있는 이유는 데모 시딩을 끄면서 관리자를 만들 유일한 경로가 사라졌기 때문이다.
 * 그래서 "만들어지는가"만이 아니라 <b>만들면 안 되는 상황에서 만들지 않는가</b>를 함께 가드한다 —
 * 특히 이미 운영 중인 관리자의 비밀번호가 재기동으로 조용히 되돌아가는 일이 없어야 한다.
 */
@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        bootstrap = new AdminBootstrap(userRepository, passwordEncoder);
        configure("admin@example.com", "s3cret-from-env");
    }

    private void configure(String email, String password) {
        ReflectionTestUtils.setField(bootstrap, "email", email);
        ReflectionTestUtils.setField(bootstrap, "password", password);
    }

    @Test
    @DisplayName("환경변수가 없으면 아무 것도 만들지 않는다")
    void doesNothingWithoutConfiguration() {
        configure("", "");

        bootstrap.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("한쪽만 채워져 있어도 만들지 않는다")
    void doesNothingWithPartialConfiguration() {
        configure("admin@example.com", "");
        bootstrap.run();

        configure("", "s3cret-from-env");
        bootstrap.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자가 이미 있으면 건너뛴다 — 재기동으로 비밀번호가 되돌아가지 않는다")
    void skipsWhenAdminAlreadyExists() {
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        bootstrap.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("그 이메일이 다른 역할로 쓰이고 있으면 중단한다")
    void stopsWhenEmailIsTaken() {
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(0L);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        bootstrap.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자가 없으면 만든다 — 비밀번호는 해시로 저장된다")
    void createsAdminWhenAbsent() {
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(0L);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

        bootstrap.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getPasswordHash()).isNotEqualTo("s3cret-from-env");
        assertThat(passwordEncoder.matches("s3cret-from-env", saved.getPasswordHash())).isTrue();
    }
}
