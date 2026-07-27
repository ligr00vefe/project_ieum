package com.project.ieum.service.password;

import com.project.ieum.entity.User;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.service.AsyncMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final AsyncMailService asyncMailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${ieum.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final long COOLDOWN_DAYS = 7;
    private static final String CHARS_UPPER  = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String CHARS_LOWER  = "abcdefghjkmnpqrstuvwxyz";
    private static final String CHARS_DIGIT  = "23456789";
    private static final String CHARS_SPECIAL = "@$!%*#?&";

    /** 이메일 → 마지막 발급 시각 (서버 재시작 시 초기화 — 소규모 서비스 전제) */
    private final Map<String, LocalDateTime> lastSentMap = new ConcurrentHashMap<>();

    public record TempPasswordResult(boolean sent, LocalDateTime nextAvailableAt) {}

    /**
     * 임시 비밀번호 발급 요청.
     * - 계정이 없으면 조용히 성공 처리 (User Enumeration 방지)
     * - 7일 쿨다운 적용
     */
    @Transactional
    public TempPasswordResult requestTempPassword(String email) {
        LocalDateTime now = LocalDateTime.now();

        // 쿨다운 체크
        LocalDateTime last = lastSentMap.get(email.toLowerCase());
        if (last != null) {
            LocalDateTime nextAvailable = last.plusDays(COOLDOWN_DAYS);
            if (now.isBefore(nextAvailable)) {
                return new TempPasswordResult(false, nextAvailable);
            }
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // 계정 없어도 발급된 것처럼 처리 (User Enumeration 방지)
            lastSentMap.put(email.toLowerCase(), now);
            return new TempPasswordResult(true, null);
        }

        User user = userOpt.get();
        String tempPassword = generateTempPassword();
        user.changePassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        lastSentMap.put(email.toLowerCase(), now);
        sendTempPasswordEmail(email, tempPassword);

        return new TempPasswordResult(true, null);
    }

    /** 쿨다운 잔여 확인 (GET /password/forgot 에서 이메일 없이 호출 불가 — 이메일 제출 후 리다이렉트로 처리) */
    public Optional<LocalDateTime> getNextAvailableAt(String email) {
        LocalDateTime last = lastSentMap.get(email.toLowerCase());
        if (last == null) return Optional.empty();
        LocalDateTime next = last.plusDays(COOLDOWN_DAYS);
        return LocalDateTime.now().isBefore(next) ? Optional.of(next) : Optional.empty();
    }

    private String generateTempPassword() {
        SecureRandom rng = new SecureRandom();
        char[] pw = new char[10];
        // 각 문자 종류 최소 1개 보장
        pw[0] = CHARS_UPPER.charAt(rng.nextInt(CHARS_UPPER.length()));
        pw[1] = CHARS_LOWER.charAt(rng.nextInt(CHARS_LOWER.length()));
        pw[2] = CHARS_DIGIT.charAt(rng.nextInt(CHARS_DIGIT.length()));
        pw[3] = CHARS_SPECIAL.charAt(rng.nextInt(CHARS_SPECIAL.length()));
        String all = CHARS_UPPER + CHARS_LOWER + CHARS_DIGIT + CHARS_SPECIAL;
        for (int i = 4; i < 10; i++) pw[i] = all.charAt(rng.nextInt(all.length()));
        // 셔플
        for (int i = 9; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = pw[i]; pw[i] = pw[j]; pw[j] = tmp;
        }
        return new String(pw);
    }

    private void sendTempPasswordEmail(String to, String tempPassword) {
        String html = """
                <div style="font-family:sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f8fafc;border-radius:16px;">
                  <h2 style="color:#4f46e5;margin-bottom:8px;">임시 비밀번호 발급 안내</h2>
                  <p style="color:#374151;line-height:1.6;">
                    아래 임시 비밀번호로 로그인 후 반드시 <strong>비밀번호를 변경</strong>해 주세요.
                  </p>
                  <div style="margin:24px 0;padding:20px 32px;background:#fff;border-radius:12px;border:2px solid #4f46e5;text-align:center;">
                    <span style="font-size:28px;font-weight:800;letter-spacing:4px;color:#4f46e5;">%s</span>
                  </div>
                  <a href="%s/login"
                     style="display:inline-block;padding:14px 28px;background:#4f46e5;color:#fff;border-radius:10px;text-decoration:none;font-weight:600;">
                    이음 로그인하기
                  </a>
                  <p style="color:#9ca3af;font-size:13px;margin-top:24px;">
                    본인이 요청하지 않았다면 즉시 고객센터로 문의해 주세요.
                  </p>
                </div>
                """.formatted(tempPassword, baseUrl);
        // 발송은 비동기로 — 동기로 보내면 SMTP가 멈추는 동안 이 트랜잭션의 DB 커넥션이 묶인다.
        asyncMailService.sendHtml(to, "[이음] 임시 비밀번호가 발급되었습니다", html);
    }

    public static String formatDateTime(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분"));
    }
}
