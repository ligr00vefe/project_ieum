package com.project.ieum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    // 채팅 목록 — 헤더 메뉴 /chat 클릭 시 진입점
    @GetMapping({"", "/"})
    public String list() {
        // TODO: 채팅 목록 페이지 구현 전까지 게시판으로 리다이렉트
        return "redirect:/board";
    }

    @GetMapping("/{id}")
    public String room(@PathVariable Long id, Model model) {
        model.addAttribute("title", "채팅");
        model.addAttribute("content", "chat/room");
        return "layout/layout";
    }
}
