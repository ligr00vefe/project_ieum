package com.project.ieum.service.password;

import com.project.ieum.entity.PasswordResetToken;
import com.project.ieum.entity.User;
import com.project.ieum.exception.BusinessException;
import com.project.ieum.repository.PasswordResetTokenRepository;
import com.project.ieum.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${ieum.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final int TOKEN_EXPIRE_MINUTES = 30;
    private static final int RATE_LIMIT_MAX = 5;
    private static final long RATE_LIMIT_WINDOW_MS = 60 * 60 * 1000L; // 1시간

    /** IP별 요청 횟수 추적 (애플리케이션 재시작 시 초기화 — 운영 규모에서는 Redis로 교체) */
    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    /**
     * 비밀번호 재설정 메일 요청.
     * 계정 존재 여부와 관계없이 동일한 처리 흐름을 유지해 User Enumeration을 방지한다.
     */
    @Transactional
    public void requestReset(String email, String clientIp) {
        checkRateLimit(clientIp);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // 계정 없어도 조용히 반환 — 호출자는 항상 "메일 발송됨" 메시지를 보여준다
            return;
        }

        User user = userOpt.get();

        // 기존 미사용 토큰 전부 무효화
        tokenRepository.deleteAllByUser(user);

        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRE_MINUTES))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        tokenRepository.save(token);
        sendResetEmail(user.getEmail(), rawToken);
    }

    /**
     * 토큰 유효성 검증. 폼 렌더링 시 사전 확인용.
     */
    @Transactional(readOnly = true)
    public boolean isTokenValid(String rawToken) {
        String tokenHash = hash(rawToken);
        return tokenRepository.findByTokenHash(tokenHash)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    /**
     * 비밀번호 실제 변경. 토큰 검증 후 변경하고 즉시 무효화.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = hash(rawToken);
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "유효하지 않은 링크입니다."));

        if (!token.isValid()) {
            throw new BusinessException("INVALID_TOKEN", "링크가 만료되었거나 이미 사용된 링크입니다.");
        }

        User user = token.getUser();
        user.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed();

        // 해당 유저의 나머지 토큰도 전부 정리
        tokenRepository.deleteAllByUser(user);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private String generateRawToken() {
        try {
            byte[] bytes = SecureRandom.getInstanceStrong().generateSeed(32);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SecureRandom 초기화 실패", e);
        }
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 지원 없음", e);
        }
    }

    private void sendResetEmail(String to, String rawToken) {
        String resetUrl = baseUrl + "/password/reset?token=" + rawToken;
        String html = """
                <div style="font-family:sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f8fafc;border-radius:16px;">
                  <h2 style="color:#4f46e5;margin-bottom:8px;">비밀번호 재설정</h2>
                  <p style="color:#374151;line-height:1.6;">아래 버튼을 클릭하여 새 비밀번호를 설정하세요.<br>
                  링크는 <strong>30분</strong> 동안만 유효합니다.</p>
                  <a href="%s"
                     style="display:inline-block;margin:24px 0;padding:14px 28px;background:#4f46e5;color:#fff;border-radius:10px;text-decoration:none;font-weight:600;">
                    비밀번호 재설정하기
                  </a>
                  <p style="color:#9ca3af;font-size:13px;">본인이 요청하지 않았다면 이 메일을 무시하세요.<br>
                  비밀번호는 변경되지 않습니다.</p>
                </div>
                """.formatted(resetUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[이음] 비밀번호 재설정 안내");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("비밀번호 재설정 메일 발송 실패: {}", to, e);
            // 메일 실패를 클라이언트에 노출하지 않음 (User Enumeration 방지)
        }
    }

    private void checkRateLimit(String clientIp) {
        long now = System.currentTimeMillis();
        RateLimitEntry entry = rateLimitMap.compute(clientIp, (ip, existing) -> {
            if (existing == null || now - existing.windowStart > RATE_LIMIT_WINDOW_MS) {
                return new RateLimitEntry(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (entry.count.get() > RATE_LIMIT_MAX) {
            throw new BusinessException("RATE_LIMIT", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private record RateLimitEntry(long windowStart, AtomicInteger count) {}
}
