package com.project.ieum.config;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 관리자 계정을 최초 1회 만든다.
 *
 * <p>회원가입은 이용자와 활동지원사만 만들고({@code UserService}), 관리 화면에도 역할을 올리는
 * 기능이 없다. 그래서 관리자는 기동 시점에 심는 수밖에 없는데, 그 자리가 원래
 * {@code DataInitializer}의 데모 시딩 안이었다. 비밀번호가 소스에 박혀 있어 그 시딩을 껐고,
 * 그 결과 <b>관리자를 만들 방법이 코드에서 사라진다</b>. 이 클래스가 그 자리를 대신한다.
 *
 * <p>데모 시딩과 분리한 것이 요점이다. 시드 스위치를 켜서 관리자를 얻으려면 소스에 공개된
 * 비밀번호를 가진 데모 계정 여럿이 함께 심어지므로, 막으려던 상태를 거쳐야 관리자를 얻게 된다.
 *
 * <p>값은 저장소에 두지 않는다. 환경변수 {@code IEUM_ADMIN_BOOTSTRAP_EMAIL} /
 * {@code IEUM_ADMIN_BOOTSTRAP_PASSWORD}로만 주입하며, 둘 중 하나라도 비면 아무 일도 하지 않는다.
 *
 * <p>판정은 이메일이 아니라 <b>ADMIN이 한 명이라도 있는지</b>로 한다. 이메일 기준이면 주소를 바꿔
 * 재기동할 때마다 관리자가 늘어난다. 이미 있으면 비밀번호도 건드리지 않는다 — 환경변수가 남은 채
 * 재기동됐다고 해서 운영 중인 계정의 비밀번호가 조용히 되돌아가면 안 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ieum.admin.bootstrap.email:}")
    private String email;

    @Value("${ieum.admin.bootstrap.password:}")
    private String password;

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (userRepository.countByRole(UserRole.ADMIN) > 0) {
            log.info("관리자 계정이 이미 있어 부트스트랩을 건너뜁니다");
            return;
        }
        if (userRepository.existsByEmail(email)) {
            log.warn("ieum.admin.bootstrap.email이 이미 다른 역할로 쓰이고 있어 부트스트랩을 중단합니다 - email={}", email);
            return;
        }

        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .phone(null)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        // 비밀번호는 남기지 않는다.
        log.info("관리자 계정 부트스트랩 완료 - email={}", email);
    }
}
