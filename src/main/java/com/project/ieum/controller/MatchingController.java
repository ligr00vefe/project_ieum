package com.project.ieum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/matching")
@RequiredArgsConstructor
public class MatchingController {

    // /matching 진입 시 게시판으로 리다이렉트
    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/board";
    }

    @GetMapping("/confirmed")
    public String confirmed(Model model) {
        model.addAttribute("title", "매칭 확정");
        model.addAttribute("content", "matching/confirmed");
        return "layout/layout";
    }
}
