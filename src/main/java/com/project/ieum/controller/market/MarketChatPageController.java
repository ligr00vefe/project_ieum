package com.project.ieum.controller.market;

import com.project.ieum.entity.User;
import com.project.ieum.entity.market.MarketChat;
import com.project.ieum.entity.market.MarketPostImage;
import com.project.ieum.service.common.CurrentUserService;
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
    public String room(@PathVariable Long chatId, Model model) {
        User currentUser = currentUserService.getCurrentUser();
        MarketChat chat = marketChatService.getChatForUser(chatId);

        // 상대방 정보 결정 (현재 사용자가 판매자면 상대는 구매자, 반대도 동일)
        boolean isSeller = chat.getSeller().getId().equals(currentUser.getId());
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
        // 거래확정 버튼 표시 여부
        model.addAttribute("sellerConfirmed", chat.isSellerConfirmed());
        model.addAttribute("buyerConfirmed", chat.isBuyerConfirmed());
        model.addAttribute("isBothConfirmed", chat.isBothConfirmed());

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

        // 각 채팅방의 마지막 메시지·대표 이미지·안읽음 수는 Service에서 통합 조회
        // (Phase 5 템플릿 작업 시 필요에 따라 추가 보완)
        model.addAttribute("title", "마켓 채팅");
        model.addAttribute("chats", chats);
        model.addAttribute("currentUserId", currentUser.getId());
        return "market/chat-list";  // templates/market/chat-list.html
    }
}