package com.project.ieum.controller.market;

import com.project.ieum.dto.market.MarketReviewForm;
import com.project.ieum.entity.market.MarketChat;
import com.project.ieum.service.market.MarketChatService;
import com.project.ieum.service.market.MarketReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/market/review")
public class MarketReviewController {

    private final MarketReviewService marketReviewService;
    private final MarketChatService marketChatService;

    // ── 후기 작성 폼 ──
    // GET /market/review/{chatId}/new
    @GetMapping("/{chatId}/new")
    public String createForm(@PathVariable Long chatId, Model model) {
        // 채팅방 정보 조회 (거래 게시글 정보 표시용)
        MarketChat chat = marketChatService.getChatForUser(chatId);

        model.addAttribute("title", "거래 후기 작성");
        model.addAttribute("chat", chat);
        model.addAttribute("post", chat.getPost());
        model.addAttribute("form", new MarketReviewForm());
        return "market/review-form";  // templates/market/review-form.html
    }

    // ── 후기 작성 처리 ──
    // POST /market/review/{chatId}
    @PostMapping("/{chatId}")
    public String create(
            @PathVariable Long chatId,
            @Valid @ModelAttribute("form") MarketReviewForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            MarketChat chat = marketChatService.getChatForUser(chatId);
            model.addAttribute("title", "거래 후기 작성");
            model.addAttribute("chat", chat);
            model.addAttribute("post", chat.getPost());
            return "market/review-form";
        }

        marketReviewService.create(chatId, form);
        redirectAttributes.addFlashAttribute("message", "후기가 등록되었습니다.");
        return "redirect:/market/chats"; // 후기 작성 후 내 채팅 목록으로
    }
}