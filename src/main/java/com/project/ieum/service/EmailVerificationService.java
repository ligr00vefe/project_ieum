package com.project.ieum.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final int CODE_EXPIRE_MINUTES = 10;
    private static final int CODE_LENGTH = 6;

    private record CodeEntry(String code, LocalDateTime expiresAt) {
        boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    }

    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    /** 6자리 인증 코드 생성 후 이메일 발송 */
    public void sendVerificationCode(String email) {
        String code = generateCode();
        codeStore.put(email, new CodeEntry(code, LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES)));
        sendCodeEmail(email, code);
    }

    /** 코드 검증 — 만료 또는 불일치 시 false */
    public boolean verify(String email, String code) {
        CodeEntry entry = codeStore.get(email);
        if (entry == null || entry.isExpired()) return false;
        if (!entry.code().equals(code.trim())) return false;
        codeStore.remove(email);
        return true;
    }

    private String generateCode() {
        SecureRandom rng = new SecureRandom();
        return String.format("%06d", rng.nextInt(1_000_000));
    }

    private void sendCodeEmail(String to, String code) {
        String html = """
                <div style="font-family:sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f8fafc;border-radius:16px;">
                  <h2 style="color:#0d9488;margin-bottom:8px;">이음 이메일 인증</h2>
                  <p style="color:#374151;line-height:1.6;">아래 인증 코드를 입력해 이메일을 인증해 주세요.<br>
                  코드는 <strong>%d분</strong> 동안 유효합니다.</p>
                  <div style="margin:24px 0;padding:20px 32px;background:#fff;border-radius:12px;border:2px solid #0d9488;text-align:center;">
                    <span style="font-size:36px;font-weight:800;letter-spacing:8px;color:#0d9488;">%s</span>
                  </div>
                  <p style="color:#9ca3af;font-size:13px;">본인이 요청하지 않았다면 이 메일을 무시하세요.</p>
                </div>
                """.formatted(CODE_EXPIRE_MINUTES, code);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[이음] 이메일 인증 코드");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("인증 코드 메일 발송 실패: {}", to, e);
            throw new RuntimeException("메일 발송에 실패했습니다. 이메일 주소를 확인해 주세요.");
        }
    }
}
