package com.project.ieum.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UserController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "이음");
        return "index";
    }

    @GetMapping("/healthz")
    @ResponseBody
    public String healthz() {
        return "ok";
    }

    @GetMapping("/readyz")
    @ResponseBody
    public String readyz() {
        return "ok";
    }
}
