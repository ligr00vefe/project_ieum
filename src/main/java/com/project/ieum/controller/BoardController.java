package com.project.ieum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    @GetMapping({"", "/"})
    public String list(Model model) {
        model.addAttribute("title", "매칭 게시판");
        model.addAttribute("content", "board/list");
        return "layout/layout";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("title", "요청 글 작성");
        model.addAttribute("content", "board/create");
        return "layout/layout";
    }

    @PostMapping("/create")
    public String createSubmit() {
        // TODO: DB 연동 후 실제 저장 처리
        return "redirect:/board";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("title", "게시물 상세");
        model.addAttribute("content", "board/detail");
        return "layout/layout";
    }

    @GetMapping("/{id}/applicants")
    public String applicants(@PathVariable Long id, Model model) {
        model.addAttribute("title", "지원자 리스트");
        model.addAttribute("content", "board/applicants");
        return "layout/layout";
    }
}
