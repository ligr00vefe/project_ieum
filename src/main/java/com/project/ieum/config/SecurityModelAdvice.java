package com.project.ieum.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 컨트롤러의 Model에 인증 상태를 자동으로 주입합니다.
 * sec:authorize 대신 순수 th:if 조건으로 사용하기 위함입니다.
 */
@ControllerAdvice
public class SecurityModelAdvice {

    /** 로그인 여부 */
    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** 장애인(이용자) 여부 */
    @ModelAttribute("isUser")
    public boolean isUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    /** 활동지원사 여부 */
    @ModelAttribute("isCaregiver")
    public boolean isCaregiver(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAREGIVER"));
    }

    /**
     * 현재 요청 URI — nav.html 등에서 active 상태 판단에 사용.
     * Thymeleaf 3.1+에서 #httpServletRequest 직접 접근이 제한되므로 모델로 주입.
     */
    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
