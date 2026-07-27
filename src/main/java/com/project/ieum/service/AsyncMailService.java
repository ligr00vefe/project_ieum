package com.project.ieum.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * HTML 메일 비동기 발송 공용 서비스.
 *
 * <p>SMTP 발송을 트랜잭션 안에서 동기로 하면, 메일 서버가 응답하지 않는 동안 그 스레드가 DB 커넥션을
 * 쥔 채로 멈춘다. 이런 스레드가 풀 크기만큼 쌓이면 커넥션 풀이 고갈되어 서비스 전체가 500으로 죽는다.
 * 그래서 발송은 항상 요청 스레드 밖(@Async)에서 처리하고, 실패는 로그만 남긴다.
 *
 * <p>본문(html)은 호출부가 트랜잭션 안에서 완성해 문자열로 넘긴다 — 엔티티를 그대로 넘기면
 * 다른 스레드에서 지연로딩이 터진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("메일 발송 실패: to={}, subject={}", to, subject, e);
        }
    }
}
