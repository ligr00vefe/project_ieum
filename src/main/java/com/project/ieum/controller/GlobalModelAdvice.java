package com.project.ieum.controller;

import com.project.ieum.entity.User;
import com.project.ieum.service.common.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final CurrentUserService currentUserService;

    @ModelAttribute("currentUser")
    public User currentUser() {
        return currentUserService.getCurrentUserOrEmpty().orElse(null);
    }

    @ModelAttribute("currentRole")
    public String currentRole() {
        return currentUserService.getCurrentUserOrEmpty()
                .map(user -> user.getRole().name())
                .orElse(null);
    }
}
