package com.project.ieum.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/board")
public class BoardController {

    @GetMapping({"", "/", "/**"})
    public String redirect(Authentication auth) {
        // 비로그인 방문자는 공개 열람이 열려 있는 활동지원사 경로 게시판으로 보낸다.
        // (/disabled/board는 이용자 전용이라 익명으로는 열리지 않는다)
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/caregiver/board";
        }
        if (auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAREGIVER"))) {
            return "redirect:/caregiver/board";
        }
        return "redirect:/disabled/board";
    }
}
