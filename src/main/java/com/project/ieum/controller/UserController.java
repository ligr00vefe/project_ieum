package com.project.ieum.controller;

import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.User;
import com.project.ieum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/how-to-use")
    public String howToUse(Model model) {
        model.addAttribute("content", "guide/how-to-use");
        model.addAttribute("title", "이용방법");
        return "layout/layout";
    }

    @GetMapping("/disabled/home")
    public String disabledHome(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("email", userDetails.getUsername());
        return "home/disabled-home";
    }

    @GetMapping("/caregiver/home")
    public String caregiverHome(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("email", userDetails.getUsername());
        return "home/caregiver-home";
    }

    @GetMapping("/mypage")
    public String mypage(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (user.getRole() == UserRole.CAREGIVER) {
            return "redirect:/caregiver/mypage";
        }
        return "redirect:/disabled/mypage";
    }

    @GetMapping("/disabled/mypage")
    public String disabledMypage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("content", "disabled/mypage");
        model.addAttribute("title", "마이페이지");
        return "layout/layout";
    }

    @GetMapping("/caregiver/mypage")
    public String caregiverMypage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("content", "caregiver/mypage");
        model.addAttribute("title", "마이페이지");
        return "layout/layout";
    }
}
