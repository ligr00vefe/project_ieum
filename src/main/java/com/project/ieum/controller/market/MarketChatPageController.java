package com.project.ieum.controller.market;

import com.project.ieum.dto.market.MarketChatSummaryResponse;
import com.project.ieum.entity.User;
import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.market.MarketChat;
import com.project.ieum.entity.market.MarketPostImage;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.repository.market.MarketReviewRepository;
import com.project.ieum.service.market.MarketChatService;
import com.project.ieum.service.market.MarketPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/market")
public class MarketChatPageController {

    private final MarketChatService marketChatService;
    private final MarketPostService marketPostService;
    private final CurrentUserService currentUserService;
    private final MarketReviewRepository marketReviewRepository;

    // ── 채팅방 열기 (없으면 생성 → 채팅방으로 리다이렉트) ──
    // POST /market/{postId}/chat
    // 상세 페이지의 "채팅하기" 버튼이 이 URL로 POST
    @PostMapping("/{postId}/chat")
    public String openChat(
            @PathVariable Long postId,
            RedirectAttributes redirectAttributes) {

        MarketChat chat = marketChatService.openOrGet(postId);
        // 채팅방 ID를 URL에 붙여 해당 채팅방으로 바로 이동
        return "redirect:/market/chat/" + chat.getId();
    }

    // ── 채팅방 페이지 ──
    // GET /market/chat/{chatId}
    // 기존 ChatPageController.room()과 동일한 구조
    @GetMapping("/chat/{chatId}")
    public String room(@PathVariable Long chatId, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");

        MarketChat chat = isAdmin
                ? marketChatService.getChatById(chatId)
                : marketChatService.getChatForUser(chatId);

        if (!isAdmin && chat.getStatus() == ConversationStatus.CLOSED) {
            redirectAttributes.addFlashAttribute("errorMessage", "신고 처리로 인해 종료된 채팅방입니다.");
            return "redirect:/market/chats";
        }

        // 상대방 정보 결정 (관리자는 판매자 기준, 일반 사용자는 상대방 기준)
        boolean isSeller = isAdmin || chat.getSeller().getId().equals(currentUser.getId());
        User opponent = isSeller ? chat.getBuyer() : chat.getSeller();

        // 게시글 대표 이미지
        List<MarketPostImage> images = marketPostService.getImages(chat.getPost().getId());
        String postThumbnail = images.isEmpty() ? null : images.get(0).getImageUrl();

        model.addAttribute("title", chat.getPost().getTitle() + " 채팅");
        model.addAttribute("chatId", chatId);
        model.addAttribute("post", chat.getPost());
        model.addAttribute("postThumbnail", postThumbnail);
        model.addAttribute("isSeller", isSeller);
        model.addAttribute("opponentEmail", opponent.getEmail());
        model.addAttribute("opponentId", opponent.getId());
        model.addAttribute("sellerConfirmed", chat.isSellerConfirmed());

        return "market/chat-room";  // templates/market/chat-room.html
    }

    // ── 내 채팅 목록 ──
    // GET /market/chats
    @GetMapping("/chats")
    public String myChats(
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        User currentUser = currentUserService.getCurrentUser();
        Page<MarketChat> chats = marketChatService.getMyChats(pageable);

        // 썸네일 포함 SummaryResponse로 변환
        Page<MarketChatSummaryResponse> chatSummaries = chats.map(chat -> {
            List<MarketPostImage> imgs = marketPostService.getImages(chat.getPost().getId());
            String thumb = imgs.isEmpty() ? null : imgs.get(0).getImageUrl();
            boolean hasReview = marketReviewRepository.existsByChat_Id(chat.getId());
            return MarketChatSummaryResponse.from(chat, currentUser.getId(), thumb, null, 0, hasReview);
        });

        model.addAttribute("title", "마켓 채팅");
        model.addAttribute("chats", chatSummaries);
        model.addAttribute("currentUserId", currentUser.getId());
        return "market/chat-list";  // templates/market/chat-list.html
    }
}