package com.project.ieum.controller.password;

import com.project.ieum.service.password.PasswordResetService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

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
            Model model,
            RedirectAttributes redirectAttributes) {

        PasswordResetService.TempPasswordResult result = passwordResetService.requestTempPassword(email);

        if (!result.sent()) {
            LocalDateTime next = result.nextAvailableAt();
            String msg = next != null
                    ? "임시 비밀번호는 " + PasswordResetService.formatDateTime(next) + " 이후 다시 재발급이 가능합니다."
                    : "잠시 후 다시 시도해 주세요.";
            redirectAttributes.addFlashAttribute("rateLimitMessage", msg);
            redirectAttributes.addFlashAttribute("lastEmail", email);
            return "redirect:/password/forgot";
        }

        redirectAttributes.addFlashAttribute("sentEmail", email);
        return "redirect:/password/sent";
    }

    @GetMapping("/sent")
    public String sent() {
        return "auth/password/sent";
    }

    /** 구 reset 링크 URL 진입 시 forgot 페이지로 안내 */
    @GetMapping("/reset")
    public String resetRedirect() {
        return "redirect:/password/forgot";
    }
}
