package com.project.ieum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/matching")
@RequiredArgsConstructor
public class MatchingController {

    @GetMapping({"", "/"})
    public String index(Authentication auth) {
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAREGIVER"))) {
            return "redirect:/caregiver/board";
        }
        return "redirect:/disabled/board";
    }

    @GetMapping("/confirmed")
    public String confirmed(Model model) {
        model.addAttribute("title", "매칭 확정");
        model.addAttribute("content", "matching/confirmed");
        return "layout/layout";
    }
}
