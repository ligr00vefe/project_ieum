package com.project.ieum.controller;

import com.project.ieum.entity.notice.Notice;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.service.admin.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        var noticePage = noticeService.getPublicPaged(page);
        int totalPages = noticePage.getTotalPages();
        model.addAttribute("notices", noticePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", Math.max(0, page - 2));
        model.addAttribute("endPage", Math.min(totalPages - 1, page + 2));
        model.addAttribute("noticePage", noticePage);
        model.addAttribute("title", "공지사항");
        model.addAttribute("description", "이음 케어 매칭 플랫폼의 공지사항입니다. 서비스 업데이트, 이벤트 등 최신 소식을 확인하세요.");
        model.addAttribute("content", "notices/list");
        return "layout/layout";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Notice notice = noticeService.getById(id);
        if (!Boolean.TRUE.equals(notice.getIsPublic())) {
            throw new NotFoundException("공지사항을 찾을 수 없습니다.");
        }
        model.addAttribute("notice", notice);
        model.addAttribute("attachments", noticeService.getAttachments(id));
        model.addAttribute("title", notice.getTitle());
        model.addAttribute("description", notice.getTitle() + " — 이음 케어 매칭 플랫폼 공지사항입니다.");
        model.addAttribute("content", "notices/detail");
        return "layout/layout";
    }
}
