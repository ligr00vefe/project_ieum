package com.project.ieum.controller.password;

import com.project.ieum.exception.BusinessException;
import com.project.ieum.service.password.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/forgot")
    public String forgotForm() {
        return "auth/password/forgot";
    }

    @PostMapping("/forgot")
    public String forgotSubmit(
            @RequestParam @Email @NotBlank String email,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String clientIp = resolveClientIp(request);
        try {
            passwordResetService.requestReset(email, clientIp);
        } catch (BusinessException e) {
            if ("RATE_LIMIT".equals(e.getCode())) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/password/forgot";
            }
            // 그 외 예외는 조용히 무시 — 항상 "발송됨" 화면으로 이동
        }
        // 계정 존재 여부와 무관하게 동일 응답 (User Enumeration 방지)
        return "redirect:/password/sent";
    }

    @GetMapping("/sent")
    public String sent() {
        return "auth/password/sent";
    }

    @GetMapping("/reset")
    public String resetForm(@RequestParam String token, Model model) {
        if (!passwordResetService.isTokenValid(token)) {
            model.addAttribute("errorMessage", "링크가 만료되었거나 유효하지 않습니다. 비밀번호 찾기를 다시 시도해 주세요.");
            return "auth/password/reset";
        }
        model.addAttribute("token", token);
        return "auth/password/reset";
    }

    @PostMapping("/reset")
    public String resetSubmit(
            @RequestParam @NotBlank String token,
            @RequestParam @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") String newPassword,
            @RequestParam @NotBlank String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            return "auth/password/reset";
        }

        try {
            passwordResetService.resetPassword(token, newPassword);
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/password/reset";
        }

        redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.");
        return "redirect:/login";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
