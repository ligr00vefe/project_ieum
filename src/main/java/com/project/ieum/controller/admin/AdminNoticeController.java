package com.project.ieum.controller.admin;

import com.project.ieum.dto.admin.NoticeForm;
import com.project.ieum.service.admin.NoticeService;
import com.project.ieum.service.common.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        var noticePage = noticeService.getAllPaged(page);
        int totalPages = noticePage.getTotalPages();
        model.addAttribute("notices", noticePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", Math.max(0, page - 2));
        model.addAttribute("endPage", Math.min(totalPages - 1, page + 2));
        model.addAttribute("activeMenu", "notices");
        model.addAttribute("title", "공지사항");
        return "admin/notices/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("notice", noticeService.getById(id));
        model.addAttribute("attachments", noticeService.getAttachments(id));
        model.addAttribute("activeMenu", "notices");
        model.addAttribute("title", "공지 상세");
        return "admin/notices/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new NoticeForm());
        model.addAttribute("activeMenu", "notices");
        model.addAttribute("title", "공지 작성");
        return "admin/notices/form";
    }

    @PostMapping(value = "/new", consumes = "multipart/form-data")
    public String createSubmit(@Valid @ModelAttribute("form") NoticeForm form,
                               BindingResult br,
                               @RequestParam(value = "attachFiles", required = false) List<MultipartFile> attachFiles,
                               Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("activeMenu", "notices");
            model.addAttribute("title", "공지 작성");
            return "admin/notices/form";
        }
        noticeService.create(form, currentUserService.getCurrentUser(), attachFiles);
        ra.addFlashAttribute("message", "공지사항이 등록되었습니다.");
        return "redirect:/admin/notices";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var notice = noticeService.getById(id);
        var form = new NoticeForm();
        form.setTitle(notice.getTitle());
        form.setBody(notice.getBody());
        form.setPinned(notice.getIsPinned());
        form.setPublished(notice.getIsPublic());
        model.addAttribute("form", form);
        model.addAttribute("noticeId", id);
        model.addAttribute("attachments", noticeService.getAttachments(id));
        model.addAttribute("activeMenu", "notices");
        model.addAttribute("title", "공지 수정");
        return "admin/notices/form";
    }

    @PostMapping(value = "/{id}/edit", consumes = "multipart/form-data")
    public String editSubmit(@PathVariable Long id,
                             @Valid @ModelAttribute("form") NoticeForm form,
                             BindingResult br,
                             @RequestParam(value = "attachFiles", required = false) List<MultipartFile> attachFiles,
                             Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("noticeId", id);
            model.addAttribute("attachments", noticeService.getAttachments(id));
            model.addAttribute("activeMenu", "notices");
            model.addAttribute("title", "공지 수정");
            return "admin/notices/form";
        }
        noticeService.update(id, form, attachFiles);
        ra.addFlashAttribute("message", "공지사항이 수정되었습니다.");
        return "redirect:/admin/notices";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        noticeService.delete(id);
        ra.addFlashAttribute("message", "공지사항이 삭제되었습니다.");
        return "redirect:/admin/notices";
    }

    @PostMapping("/{id}/pin")
    public String togglePin(@PathVariable Long id) {
        noticeService.togglePin(id);
        return "redirect:/admin/notices";
    }

    @PostMapping("/{id}/visibility")
    public String toggleVisibility(@PathVariable Long id) {
        noticeService.togglePublic(id);
        return "redirect:/admin/notices";
    }

    @PostMapping("/{noticeId}/files/{fileId}/delete")
    public String deleteFile(@PathVariable Long noticeId, @PathVariable Long fileId, RedirectAttributes ra) {
        noticeService.deleteAttachment(noticeId, fileId);
        ra.addFlashAttribute("message", "파일이 삭제되었습니다.");
        return "redirect:/admin/notices/" + noticeId + "/edit";
    }

    /** CKEditor 5 이미지 업로드 — {"url": "..."} 반환 */
    @PostMapping("/editor-image")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadEditorImage(
            @RequestParam("upload") MultipartFile image) {
        String url = noticeService.uploadEditorImage(image);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
