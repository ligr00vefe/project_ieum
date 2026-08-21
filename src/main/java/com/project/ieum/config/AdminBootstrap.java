package com.project.ieum.config;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${IEUM_ADMIN_BOOTSTRAP_EMAIL:}")
    private String email;

    @Value("${IEUM_ADMIN_BOOTSTRAP_PASSWORD:}")
    private String password;

    @Override
    @Transactional
    public void run(String... args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return;
        if (userRepository.countByRole(UserRole.ADMIN) > 0) {
            log.info("관리자 부트스트랩 생략: 기존 관리자 계정이 존재합니다.");
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("IEUM_ADMIN_BOOTSTRAP_PASSWORD는 12자 이상이어야 합니다.");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("관리자 부트스트랩 이메일이 이미 사용 중입니다.");
        }
        userRepository.save(User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        log.info("환경변수 기반 관리자 계정을 생성했습니다. email={}", normalizedEmail);
    }
}
