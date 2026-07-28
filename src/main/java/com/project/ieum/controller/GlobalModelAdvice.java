package com.project.ieum.controller;

import com.project.ieum.entity.User;
import com.project.ieum.service.admin.AdminPopupService;
import com.project.ieum.service.common.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final CurrentUserService currentUserService;
    private final AdminPopupService adminPopupService;

    /**
     * {@code /api/**}는 JSON·SSE 응답이라 여기서 넣는 모델 속성을 쓰지 않는다. 그런데도
     * {@code @ModelAttribute}는 @RestController 요청에도 그대로 실행되어 DB를 조회한다.
     *
     * <p>OSIV(open-in-view=true)가 Hibernate 세션을 응답 완료까지 열어 두므로, 이 조회가 잡은 커넥션은
     * 트랜잭션이 아니라 <b>세션이 닫힐 때</b> 반납된다. 일반 요청은 수백 ms 안에 끝나 티가 안 나지만
     * SSE 응답({@code /api/notifications/stream})은 30분짜리라 구독 하나당 커넥션 하나가 30분씩 물리고,
     * EventSource 자동 재연결로 반복되어 풀이 고갈된다(2026-07-28 운영 장애: active=20/idle=0로 15분 정지).
     */
    private boolean skipsModel(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    @ModelAttribute("currentUser")
    public User currentUser(HttpServletRequest request) {
        if (skipsModel(request)) return null;
        return currentUserService.getCurrentUserOrEmpty().orElse(null);
    }

    @ModelAttribute("currentRole")
    public String currentRole(HttpServletRequest request) {
        if (skipsModel(request)) return null;
        return currentUserService.getCurrentUserOrEmpty()
                .map(user -> user.getRole().name())
                .orElse(null);
    }

    @ModelAttribute("activePopups")
    public java.util.List<com.project.ieum.entity.popup.Popup> activePopups(HttpServletRequest request) {
        if (skipsModel(request)) return java.util.List.of();
        return adminPopupService.getActivePopups();
    }

    // 로그인 필요 페이지, 폼 페이지는 검색엔진 색인 제외
    @ModelAttribute("noindex")
    public boolean noindex(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/mypage") ||
               uri.startsWith("/caregiver/mypage") ||
               uri.startsWith("/disabled/mypage") ||
               uri.contains("/create") ||
               uri.contains("/edit") ||
               uri.contains("/new") ||
               uri.contains("/repost") ||
               uri.startsWith("/register") ||
               uri.startsWith("/calendar") ||
               uri.startsWith("/matching") ||
               uri.startsWith("/chat") ||
               uri.startsWith("/inquiries") ||
               uri.startsWith("/request/my") ||
               uri.startsWith("/market/my") ||
               uri.startsWith("/review/") ||
               uri.startsWith("/admin");
    }
}
