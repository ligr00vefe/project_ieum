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

@Controller
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("notices", noticeService.getAll()
                .stream()
                .filter(n -> Boolean.TRUE.equals(n.getIsPublic()))
                .toList());
        model.addAttribute("title", "공지사항");
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
        model.addAttribute("content", "notices/detail");
        return "layout/layout";
    }
}
