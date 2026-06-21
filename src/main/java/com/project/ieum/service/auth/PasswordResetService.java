package com.project.ieum.service.auth;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.auth.PasswordResetToken;
import com.project.ieum.repository.PasswordResetTokenRepository;
import com.project.ieum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * 비밀번호 재설정 흐름. 보안 요구(이슈 #56):
 * - 이메일 열거 방지: 존재 여부와 무관하게 호출측이 동일 응답을 주도록, 미존재/탈퇴 계정엔 조용히 무시.
 * - 토큰: SecureRandom 고엔트로피, 단시간 만료, 1회용.
 * - 비밀번호: BCrypt 재인코딩(평문 저장 금지), 기존 인코더 재사용.
 * - 토큰/비밀번호는 로깅하지 않는다(CLAUDE.md §14).
 *
 * 메일 발송은 단계적(옵션 B): 토큰/흐름을 먼저 구축하고, 실제 발송은 인프라 준비 시 연결한다.
 * 그 전까지 재설정 링크는 dev에서만 화면으로 노출(컨트롤러의 config 플래그)하며 여기서 반환만 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 재설정을 요청한다. 이메일이 실제 가입 계정이면 토큰을 발급하고 재설정 경로를 반환한다.
     * 미존재/탈퇴 계정이면 {@link Optional#empty()} — 호출측은 결과와 무관하게 동일 안내를 보여 열거를 막는다.
     */
    public Optional<String> requestReset(String email) {
        Optional<User> found = userRepository.findByEmail(email)
                .filter(user -> user.getStatus() != UserStatus.DELETED);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        User user = found.get();
        String token = generateToken();
        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plus(TOKEN_TTL))
                .build());
        // 토큰 값은 로그에 남기지 않는다. 발급 사실만 기록.
        log.info("[password-reset] 재설정 토큰 발급 - userId={}", user.getId());
        return Optional.of("/password/reset?token=" + token);
    }

    @Transactional(readOnly = true)
    public boolean isValidToken(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetToken::isUsable)
                .orElse(false);
    }

    /**
     * 토큰을 검증하고 비밀번호를 재설정한다. 토큰은 즉시 사용 처리(1회용).
     * @throws IllegalArgumentException 토큰이 없거나 이미 사용/만료된 경우
     */
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 링크입니다. 다시 요청해 주세요."));
        if (!resetToken.isUsable()) {
            throw new IllegalArgumentException("이미 사용되었거나 만료된 링크입니다. 다시 요청해 주세요.");
        }
        User user = resetToken.getUser();
        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetToken.markUsed();
        tokenRepository.save(resetToken);
        log.info("[password-reset] 비밀번호 재설정 완료 - userId={}", user.getId());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
