package com.project.ieum.controller;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.service.HelpRequestService;
import com.project.ieum.service.MatchingService;
import com.project.ieum.service.common.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserService currentUserService;
    private final HelpRequestService helpRequestService;
    private final MatchingService matchingService;

    @GetMapping("/me")
    public String me(Model model) {
        User currentUser = currentUserService.getCurrentUser();
        model.addAttribute("title", "마이페이지");
        if (currentUser.getRole() == UserRole.USER) {
            model.addAttribute("requests", helpRequestService.getMyRequests());
            return "me/user";
        }
        if (currentUser.getRole() == UserRole.CAREGIVER) {
            model.addAttribute("applications", matchingService.getMyApplications());
            return "me/caregiver";
        }
        return "redirect:/admin";
    }
}
