package com.project.ieum.controller.request;

import com.project.ieum.dto.request.ReviewForm;
import com.project.ieum.service.HelpRequestService;
import com.project.ieum.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final HelpRequestService helpRequestService;

    @GetMapping("/requests/{requestId}/new")
    public String createForm(@PathVariable Long requestId, Model model) {
        model.addAttribute("title", "후기 작성");
        model.addAttribute("request", helpRequestService.get(requestId));
        model.addAttribute("form", new ReviewForm());
        return "review/form";
    }

    @PostMapping("/requests/{requestId}")
    public String create(
            @PathVariable Long requestId,
            @Valid @ModelAttribute("form") ReviewForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "후기 작성");
            model.addAttribute("request", helpRequestService.get(requestId));
            return "review/form";
        }
        reviewService.create(requestId, form);
        redirectAttributes.addFlashAttribute("message", "후기가 등록되었습니다.");
        return "redirect:/disabled/mypage";
    }
}
