package com.project.ieum.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WelcomeEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${ieum.base-url:http://localhost:8080}")
    private String baseUrl;

    @Async
    public void sendWelcome(String to, String name, boolean isCaregiver) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[이음] 가입을 환영합니다! 🎉");
            helper.setText(buildHtml(name, isCaregiver), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("환영 메일 발송 실패: to={}", to, e);
        }
    }

    private String buildHtml(String name, boolean isCaregiver) {
        String roleColor   = isCaregiver ? "#0d9488" : "#4f46e5";
        String roleBg      = isCaregiver ? "#f0fdfa" : "#eef2ff";
        String roleName    = isCaregiver ? "활동지원사" : "케어메이트";
        String roleDesc    = isCaregiver
                ? "활동지원사로 등록되셨습니다.<br>이음에서 도움이 필요한 분들과 연결되어 보세요."
                : "케어메이트로 등록되셨습니다.<br>이음에서 믿을 수 있는 활동지원사를 찾아보세요.";
        String ctaText     = isCaregiver ? "매칭 게시판 둘러보기" : "활동지원사 찾아보기";
        String ctaUrl      = baseUrl + (isCaregiver ? "/caregiver/board" : "/disabled/board");

        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Apple SD Gothic Neo',AppleGothic,'Malgun Gothic',sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px;">
                  <tr><td align="center">
                    <table width="520" cellpadding="0" cellspacing="0" style="max-width:520px;width:100%%;">

                      <!-- 로고 헤더 -->
                      <tr>
                        <td align="center" style="padding-bottom:24px;">
                          <table cellpadding="0" cellspacing="0">
                            <tr>
                              <td style="background:%s;border-radius:16px;width:44px;height:44px;text-align:center;vertical-align:middle;">
                                <span style="font-size:22px;line-height:44px;">♥</span>
                              </td>
                              <td style="padding-left:10px;vertical-align:middle;">
                                <div style="font-size:20px;font-weight:800;color:#111827;letter-spacing:-0.5px;">이음</div>
                                <div style="font-size:10px;color:#9ca3af;letter-spacing:2px;margin-top:-2px;">CARE MATCH</div>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>

                      <!-- 히어로 배너 -->
                      <tr>
                        <td style="background:linear-gradient(135deg,%s 0%%,#818cf8 100%%);border-radius:24px 24px 0 0;padding:40px 32px 36px;text-align:center;">
                          <!-- SVG 일러스트 -->
                          <svg width="80" height="80" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg" style="margin-bottom:20px;">
                            <circle cx="40" cy="40" r="40" fill="rgba(255,255,255,0.15)"/>
                            <circle cx="40" cy="40" r="28" fill="rgba(255,255,255,0.2)"/>
                            <path d="M40 22c-2.8 0-5 2.2-5 5s2.2 5 5 5 5-2.2 5-5-2.2-5-5-5z" fill="white"/>
                            <path d="M40 35c-6.6 0-12 5.4-12 12v1h24v-1c0-6.6-5.4-12-12-12z" fill="white"/>
                            <path d="M52 34l3 3 6-6" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                          </svg>
                          <div style="font-size:28px;font-weight:800;color:#fff;line-height:1.2;margin-bottom:8px;">
                            환영합니다,<br>%s님! 🎉
                          </div>
                          <div style="font-size:14px;color:rgba(255,255,255,0.85);line-height:1.6;">
                            이음 <strong>%s</strong> 회원가입이 완료되었습니다.
                          </div>
                        </td>
                      </tr>

                      <!-- 본문 카드 -->
                      <tr>
                        <td style="background:#ffffff;border-radius:0 0 24px 24px;padding:32px;">

                          <!-- 역할 배지 -->
                          <div style="display:inline-block;background:%s;color:%s;font-size:12px;font-weight:700;padding:6px 14px;border-radius:20px;margin-bottom:20px;">
                            %s 회원
                          </div>

                          <p style="font-size:15px;color:#374151;line-height:1.8;margin:0 0 24px;">
                            %s
                          </p>

                          <!-- 서비스 소개 3단 -->
                          <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:28px;">
                            <tr>
                              <td width="33%%" style="text-align:center;padding:16px 8px;background:#f8fafc;border-radius:12px;margin:0 4px;">
                                <div style="font-size:24px;margin-bottom:6px;">🤝</div>
                                <div style="font-size:12px;font-weight:700;color:#111827;margin-bottom:4px;">신뢰 매칭</div>
                                <div style="font-size:11px;color:#6b7280;line-height:1.5;">검증된 활동지원사와<br>안전하게 연결</div>
                              </td>
                              <td width="4%%"></td>
                              <td width="33%%" style="text-align:center;padding:16px 8px;background:#f8fafc;border-radius:12px;">
                                <div style="font-size:24px;margin-bottom:6px;">💬</div>
                                <div style="font-size:12px;font-weight:700;color:#111827;margin-bottom:4px;">실시간 채팅</div>
                                <div style="font-size:11px;color:#6b7280;line-height:1.5;">매칭 전 충분한<br>사전 상담 가능</div>
                              </td>
                              <td width="4%%"></td>
                              <td width="33%%" style="text-align:center;padding:16px 8px;background:#f8fafc;border-radius:12px;">
                                <div style="font-size:24px;margin-bottom:6px;">⭐</div>
                                <div style="font-size:12px;font-weight:700;color:#111827;margin-bottom:4px;">후기 시스템</div>
                                <div style="font-size:11px;color:#6b7280;line-height:1.5;">투명한 후기로<br>더 나은 선택</div>
                              </td>
                            </tr>
                          </table>

                          <!-- CTA 버튼 -->
                          <table width="100%%" cellpadding="0" cellspacing="0">
                            <tr>
                              <td align="center">
                                <a href="%s"
                                   style="display:inline-block;background:%s;color:#ffffff;font-size:15px;font-weight:700;padding:16px 40px;border-radius:14px;text-decoration:none;letter-spacing:-0.3px;">
                                  %s →
                                </a>
                              </td>
                            </tr>
                          </table>

                        </td>
                      </tr>

                      <!-- 푸터 -->
                      <tr>
                        <td style="padding:24px 16px 8px;text-align:center;">
                          <p style="font-size:12px;color:#9ca3af;line-height:1.8;margin:0;">
                            본 메일은 이음 Care Match 서비스 가입으로 인해 발송되었습니다.<br>
                            <a href="%s" style="color:#9ca3af;">ieumcare.shop</a>
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(
                roleColor,       // 로고 배경
                roleColor,       // 히어로 그라디언트 시작
                name,            // 환영 이름
                roleName,        // 역할명
                roleBg,          // 배지 배경
                roleColor,       // 배지 텍스트
                roleName,        // 배지 라벨
                roleDesc,        // 역할 설명
                ctaUrl,          // CTA 링크
                roleColor,       // CTA 버튼 색
                ctaText,         // CTA 텍스트
                baseUrl          // 푸터 링크
        );
    }
}
