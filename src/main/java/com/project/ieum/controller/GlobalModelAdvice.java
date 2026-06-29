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

    @ModelAttribute("currentUser")
    public User currentUser() {
        return currentUserService.getCurrentUserOrEmpty().orElse(null);
    }

    @ModelAttribute("currentRole")
    public String currentRole() {
        return currentUserService.getCurrentUserOrEmpty()
                .map(user -> user.getRole().name())
                .orElse(null);
    }

    @ModelAttribute("activePopups")
    public java.util.List<com.project.ieum.entity.popup.Popup> activePopups() {
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
