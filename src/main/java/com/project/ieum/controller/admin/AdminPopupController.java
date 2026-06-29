package com.project.ieum.controller.admin;

import com.project.ieum.dto.admin.PopupForm;
import com.project.ieum.service.admin.AdminPopupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/admin/popups")
@RequiredArgsConstructor
public class AdminPopupController {

    private final AdminPopupService popupService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        var popupPage = popupService.getPopups(PageRequest.of(page, 10));
        int totalPages = popupPage.getTotalPages();
        model.addAttribute("popups", popupPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", Math.max(0, page - 2));
        model.addAttribute("endPage", Math.min(totalPages - 1, page + 2));
        model.addAttribute("activeMenu", "popups");
        model.addAttribute("title", "팝업 관리");
        return "admin/popups/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var popup = popupService.getPopup(id);
        model.addAttribute("popup", popup);
        model.addAttribute("activeMenu", "popups");
        model.addAttribute("title", "팝업 상세");
        return "admin/popups/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new PopupForm());
        model.addAttribute("activeMenu", "popups");
        model.addAttribute("title", "팝업 작성");
        return "admin/popups/form";
    }

    @PostMapping("/new")
    public String createSubmit(@Valid @ModelAttribute("form") PopupForm form,
                               BindingResult br,
                               Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("activeMenu", "popups");
            model.addAttribute("title", "팝업 작성");
            return "admin/popups/form";
        }
        LocalDateTime expiresAt = calculateExpiresAt(form.getDuration());
        popupService.createPopup(form.getName(), form.getContent(), form.getDuration(), expiresAt, form.getLayout(), form.getLinkUrl());
        ra.addFlashAttribute("message", "팝업이 등록되었습니다.");
        return "redirect:/admin/popups";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var popup = popupService.getPopup(id);
        var form = new PopupForm();
        form.setName(popup.getName());
        form.setContent(popup.getContent());
        form.setDuration(popup.getDuration());
        form.setLayout(popup.getLayout() != null ? popup.getLayout() : "AUTO");
        form.setLinkUrl(popup.getLinkUrl());
        model.addAttribute("form", form);
        model.addAttribute("popupId", id);
        model.addAttribute("popup", popup);
        model.addAttribute("activeMenu", "popups");
        model.addAttribute("title", "팝업 수정");
        return "admin/popups/form";
    }

    @PostMapping("/{id}/edit")
    public String editSubmit(@PathVariable Long id,
                             @Valid @ModelAttribute("form") PopupForm form,
                             BindingResult br,
                             Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("popupId", id);
            model.addAttribute("popup", popupService.getPopup(id));
            model.addAttribute("activeMenu", "popups");
            model.addAttribute("title", "팝업 수정");
            return "admin/popups/form";
        }
        LocalDateTime expiresAt = calculateExpiresAt(form.getDuration());
        popupService.updatePopup(id, form.getName(), form.getContent(), form.getDuration(), expiresAt, form.getLayout(), form.getLinkUrl());
        ra.addFlashAttribute("message", "팝업이 수정되었습니다.");
        return "redirect:/admin/popups";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        popupService.deletePopup(id);
        ra.addFlashAttribute("message", "팝업이 삭제되었습니다.");
        return "redirect:/admin/popups";
    }

    @PostMapping("/{id}/toggle")
    public String toggleEnabled(@PathVariable Long id) {
        popupService.togglePopupEnabled(id);
        return "redirect:/admin/popups";
    }

    /** CKEditor 5 이미지 업로드 — {"url": "..."} 반환 */
    @PostMapping("/editor-image")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadEditorImage(
            @RequestParam("upload") MultipartFile image) {
        String url = popupService.uploadEditorImage(image);
        return ResponseEntity.ok(Map.of("url", url));
    }

    private LocalDateTime calculateExpiresAt(String duration) {
        LocalDateTime now = LocalDateTime.now();
        return switch (duration) {
            case "HOUR_1" -> now.plusHours(1);
            case "HOUR_24" -> now.plusHours(24);
            case "WEEK_1" -> now.plusWeeks(1);
            case "MONTH_1" -> now.plusMonths(1);
            default -> now.plusHours(24);
        };
    }
}
