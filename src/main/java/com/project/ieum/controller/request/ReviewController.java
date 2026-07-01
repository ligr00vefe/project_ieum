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
        model.addAttribute("content", "review/form");
        return "layout/layout";
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
            model.addAttribute("content", "review/form");
            return "layout/layout";
        }
        reviewService.create(requestId, form);
        redirectAttributes.addFlashAttribute("message", "후기가 등록되었습니다.");
        return "redirect:/reviews/my";
    }

    @GetMapping("/my")
    public String myReviews(Model model) {
        model.addAttribute("title", "내가 쓴 후기");
        model.addAttribute("reviews", reviewService.getMyReviews());
        return "review/list";
    }

    @GetMapping("/{reviewId}")
    public String detail(@PathVariable Long reviewId, Model model) {
        model.addAttribute("title", "후기 상세");
        model.addAttribute("review", reviewService.getById(reviewId));
        return "review/detail";
    }

    @GetMapping("/{reviewId}/edit")
    public String editForm(@PathVariable Long reviewId, Model model) {
        model.addAttribute("title", "후기 수정");
        model.addAttribute("review", reviewService.getById(reviewId));
        model.addAttribute("form", new com.project.ieum.dto.request.ReviewForm());
        return "review/edit";
    }

    @PostMapping("/{reviewId}/edit")
    public String edit(@PathVariable Long reviewId,
                       @Valid @ModelAttribute("form") ReviewForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "후기 수정");
            model.addAttribute("review", reviewService.getById(reviewId));
            return "review/edit";
        }
        reviewService.update(reviewId, form);
        redirectAttributes.addFlashAttribute("message", "후기가 수정되었습니다.");
        return "redirect:/reviews/" + reviewId;
    }

    @PostMapping("/{reviewId}/delete")
    public String delete(@PathVariable Long reviewId, RedirectAttributes redirectAttributes) {
        reviewService.delete(reviewId);
        redirectAttributes.addFlashAttribute("message", "후기가 삭제되었습니다.");
        return "redirect:/reviews/my";
    }
}
