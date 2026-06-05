package com.project.ieum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/caregiver/board")
@RequiredArgsConstructor
public class CaregiverBoardController {

    @GetMapping({"", "/"})
    public String list(Model model) {
        model.addAttribute("title", "매칭 게시판");
        model.addAttribute("content", "caregiver/board/list");
        return "layout/layout";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("title", "게시물 상세");
        model.addAttribute("content", "caregiver/board/detail");
        return "layout/layout";
    }
}
