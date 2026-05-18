package com.project.ieum.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

  @GetMapping({ "/", "" })
  public String home(Model model) {
    model.addAttribute("title", "메인");
    model.addAttribute("content", "home/index");
    return "layout/layout";
  }
}
