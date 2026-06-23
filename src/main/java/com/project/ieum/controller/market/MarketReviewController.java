package com.project.ieum.controller.market;

import com.project.ieum.dto.market.MarketReviewForm;
import com.project.ieum.entity.market.MarketChat;
import com.project.ieum.entity.market.MarketPostImage;
import com.project.ieum.repository.market.MarketReviewRepository;
import com.project.ieum.service.market.MarketChatService;
import com.project.ieum.service.market.MarketPostService;
import com.project.ieum.service.market.MarketReviewService;

import java.util.List;
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
    private final MarketPostService marketPostService;
    private final MarketReviewRepository marketReviewRepository;

    // ── 후기 작성 폼 ──
    // GET /market/review/{chatId}/new
    @GetMapping("/{chatId}/new")
    public String createForm(@PathVariable Long chatId, Model model, RedirectAttributes redirectAttributes) {
        // 이미 후기를 작성한 경우 차단
        if (marketReviewRepository.existsByChat_Id(chatId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 후기를 작성한 거래입니다.");
            return "redirect:/market/chats";
        }

        MarketChat chat = marketChatService.getChatForUser(chatId);

        List<MarketPostImage> imgs = marketPostService.getImages(chat.getPost().getId());
        String thumbnailUrl = imgs.isEmpty() ? null : imgs.get(0).getImageUrl();

        model.addAttribute("title", "거래 후기 작성");
        model.addAttribute("chat", chat);
        model.addAttribute("post", chat.getPost());
        model.addAttribute("thumbnailUrl", thumbnailUrl);
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