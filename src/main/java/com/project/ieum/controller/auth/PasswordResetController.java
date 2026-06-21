package com.project.ieum.controller.auth;

import com.project.ieum.service.auth.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * 비밀번호 찾기/재설정. 비인증 사용자가 접근하므로 /password/** 는 permitAll.
 * 신규 비밀번호 변경 흐름과 분리해 기존 로그인/회원 컨트롤러는 건드리지 않는다.
 */
@Controller
@RequestMapping("/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // 메일 인프라 전(옵션 B)에는 dev에서만 재설정 링크를 화면에 노출한다. 기본 false(프로덕션 안전).
    @Value("${app.password-reset.expose-link:false}")
    private boolean exposeResetLink;

    @GetMapping("/forgot")
    public String forgotForm(Model model) {
        model.addAttribute("title", "비밀번호 찾기");
        return "auth/password-forgot";
    }

    @PostMapping("/forgot")
    public String requestReset(@RequestParam String email, RedirectAttributes redirectAttributes) {
        Optional<String> resetPath = passwordResetService.requestReset(email);
        // 이메일 열거 방지: 존재 여부와 무관하게 동일 안내.
        redirectAttributes.addFlashAttribute("sent", true);
        if (exposeResetLink) {
            resetPath.ifPresent(path -> redirectAttributes.addFlashAttribute("devResetLink", path));
        }
        return "redirect:/password/forgot";
    }

    @GetMapping("/reset")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("title", "비밀번호 재설정");
        model.addAttribute("token", token);
        model.addAttribute("validToken", token != null && passwordResetService.isValidToken(token));
        return "auth/password-reset";
    }

    @PostMapping("/reset")
    public String reset(@RequestParam String token,
                        @RequestParam String password,
                        @RequestParam String confirmPassword,
                        RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "새 비밀번호가 일치하지 않습니다.");
            return "redirect:/password/reset?token=" + token;
        }
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호는 8자 이상이어야 합니다.");
            return "redirect:/password/reset?token=" + token;
        }
        try {
            passwordResetService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute("resetSuccess", true);
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/password/forgot";
        }
    }
}
