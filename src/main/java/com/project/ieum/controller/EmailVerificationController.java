package com.project.ieum.controller;

import com.project.ieum.service.EmailVerificationService;
import com.project.ieum.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final UserService userService;

    private static final String SESSION_KEY = "emailVerified";

    @Value("${ieum.dev.email-verify-skip-domain:}")
    private String skipDomain;

    private boolean isSkipEmail(String email) {
        return !skipDomain.isBlank() && email.toLowerCase().endsWith("@" + skipDomain.toLowerCase());
    }

    /** 인증 코드 발송 — 중복 체크 먼저, 스킵 도메인이면 즉시 인증 완료 응답 */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일을 입력해 주세요."));
        }

        email = email.trim();

        // 이메일 중복 체크 (메일 발송 전에 먼저 확인)
        if (userService.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "이미 사용 중인 이메일입니다."));
        }

        if (isSkipEmail(email)) {
            session.setAttribute(SESSION_KEY, email);
            session.setAttribute(SESSION_KEY + "At", LocalDateTime.now());
            return ResponseEntity.ok(Map.of("ok", true, "skipped", true));
        }
        try {
            emailVerificationService.sendVerificationCode(email);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "인증 메일을 발송하지 못했습니다."));
        }
    }

    /** 코드 검증 — 스킵 도메인이면 코드 없이 통과 */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        String code  = body.get("code");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일을 입력해 주세요."));
        }
        if (isSkipEmail(email.trim())) {
            session.setAttribute(SESSION_KEY, email.trim());
            session.setAttribute(SESSION_KEY + "At", LocalDateTime.now());
            return ResponseEntity.ok(Map.of("ok", true));
        }
        if (code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "인증 코드를 입력해 주세요."));
        }
        boolean ok = emailVerificationService.verify(email.trim(), code.trim());
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("error", "인증 코드가 올바르지 않거나 만료되었습니다."));
        }
        session.setAttribute(SESSION_KEY, email.trim());
        session.setAttribute(SESSION_KEY + "At", LocalDateTime.now());
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
