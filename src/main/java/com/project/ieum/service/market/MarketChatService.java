package com.project.ieum.service.market;

import com.project.ieum.dto.market.MarketMessageResponse;
import com.project.ieum.entity.User;
import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.market.MarketChat;
import com.project.ieum.entity.market.MarketMessage;
import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.market.MarketPostStatus;
import com.project.ieum.entity.notification.NotificationType;
import com.project.ieum.exception.ForbiddenException;
import com.project.ieum.exception.NotFoundException;

import com.project.ieum.repository.market.MarketChatRepository;
import com.project.ieum.repository.market.MarketMessageRepository;
import com.project.ieum.repository.market.MarketPostRepository;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.notification.NotificationService;
import com.project.ieum.util.UserDisplayName;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MarketChatService {

    private final MarketChatRepository marketChatRepository;
    private final MarketPostRepository marketPostRepository;
    private final MarketMessageRepository marketMessageRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService; // 기존 NotificationService 재사용

    // ── 채팅방 열기 (없으면 생성, 있으면 기존 반환) ──
    // MatchingService.apply()의 채팅방 생성 로직 참고
    // 구매자가 "채팅하기" 버튼을 누를 때 호출
    public MarketChat openOrGet(Long postId) {
        User currentUser = currentUserService.getCurrentUser();

        MarketPost post = marketPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        // 자기 게시글에는 채팅 불가
        if (post.getSeller().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("본인의 게시글에는 채팅할 수 없습니다.");
        }

        // REMOVED 게시글에는 채팅 불가
        if (post.getStatus() == MarketPostStatus.REMOVED) {
            throw new IllegalStateException("삭제된 게시글입니다.");
        }

        // 이미 채팅방이 있으면 기존 채팅방 반환 (중복 생성 방지)
        // DB unique 제약(uq_mc_post_buyer)과 이중으로 방어
        return marketChatRepository.findByPost_IdAndBuyer(postId, currentUser)
                .orElseGet(() -> {
                    // 신규 채팅방 생성
                    MarketChat chat = marketChatRepository.save(MarketChat.builder()
                            .post(post)
                            .seller(post.getSeller())
                            .buyer(currentUser)
                            .status(ConversationStatus.ACTIVE)
                            .lastMessageAt(LocalDateTime.now())
                            .build());

                    // 판매자에게 새 채팅 알림
                    notificationService.create(
                            post.getSeller(),
                            NotificationType.MESSAGE, // 기존 MESSAGE 타입 재사용
                            "새 채팅이 왔어요",
                            UserDisplayName.of(currentUser) + "님이 '" + post.getTitle() + "' 상품에 채팅을 시작했습니다.",
                            "/market/chat/" + chat.getId()
                    );

                    return chat;
                });
    }

    // ── 채팅방 상세 조회 (관리자용, 참여자 검증 없음) ──
    @Transactional(readOnly = true)
    public MarketChat getChatById(Long chatId) {
        return marketChatRepository.findWithDetailById(chatId)
                .orElseThrow(() -> new NotFoundException("채팅방을 찾을 수 없습니다."));
    }

    // ── 채팅방 상세 조회 (참여자 검증 포함) ──
    @Transactional(readOnly = true)
    public MarketChat getChatForUser(Long chatId) {
        User currentUser = currentUserService.getCurrentUser();
        MarketChat chat = marketChatRepository.findWithDetailById(chatId)
                .orElseThrow(() -> new NotFoundException("채팅방을 찾을 수 없습니다."));

        // 판매자 또는 구매자만 접근 가능
        if (!isParticipant(chat, currentUser.getId())) {
            throw new ForbiddenException("채팅방 참여자만 접근할 수 있습니다.");
        }
        return chat;
    }

    // ── 메시지 전송 ──
    // 기존 ChatService.sendMessage()와 동일한 로직 — MarketChat용으로 재구현
    public MarketMessageResponse sendMessage(Long chatId, String body) {
        User currentUser = currentUserService.getCurrentUser();
        MarketChat chat = getChatForUser(chatId);

        if (chat.getStatus() != ConversationStatus.ACTIVE) {
            throw new IllegalStateException("종료된 채팅방에는 메시지를 보낼 수 없습니다.");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("메시지를 입력해주세요.");
        }

        MarketMessage message = MarketMessage.builder()
                .chat(chat)
                .sender(currentUser)
                .body(body.trim())
                .hasRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        MarketMessage saved = marketMessageRepository.save(message);
        chat.touchLastMessage();

        // 상대방 알림
        User receiver = getOpponent(chat, currentUser.getId());
        notificationService.create(
                receiver,
                NotificationType.MESSAGE,
                "새 메시지",
                UserDisplayName.of(currentUser) + "님이 메시지를 보냈습니다.",
                "/market/chat/" + chatId
        );

        return MarketMessageResponse.from(saved, currentUser.getId());
    }

    // ── 메시지 목록 조회 ──
    @Transactional(readOnly = true)
    public Page<MarketMessageResponse> getMessages(Long chatId, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        getChatForUser(chatId); // 참여자 검증
        return marketMessageRepository.findByChat_IdOrderBySentAtDesc(chatId, pageable)
                .map(m -> MarketMessageResponse.from(m, currentUser.getId()));
    }

    // ── 읽음 처리 ──
    public int markRead(Long chatId) {
        User currentUser = currentUserService.getCurrentUser();
        getChatForUser(chatId); // 참여자 검증
        return marketMessageRepository.markRead(chatId, currentUser.getId());
    }

    // ── 거래 확정 (핸드셰이크) ──
    // MatchingService.confirmEnd()와 동일한 패턴
    // 판매자 또는 구매자가 "거래완료" 버튼 클릭 → 양쪽 모두 클릭하면 자동으로 SOLD 처리
    public void confirmPurchase(Long chatId) {
        User currentUser = currentUserService.getCurrentUser();
        MarketChat chat = getChatForUser(chatId);

        if (chat.getStatus() != ConversationStatus.ACTIVE) {
            throw new IllegalStateException("진행 중인 거래만 확정할 수 있습니다.");
        }

        // 판매자만 거래완료 처리 가능
        if (!chat.getSeller().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("판매자만 거래완료를 처리할 수 있습니다.");
        }
        if (chat.isSellerConfirmed()) {
            throw new IllegalStateException("이미 거래완료를 누르셨습니다.");
        }
        chat.confirmBySeller();

        // 판매자 확정 즉시 → 게시글 상태 SOLD 전환 + 알림
        {
            chat.getPost().complete();
            chat.close();

            // 양쪽에게 거래완료 알림
            String postTitle = chat.getPost().getTitle();
            notificationService.create(
                    chat.getSeller(), NotificationType.MESSAGE,
                    "거래가 완료되었어요",
                    "'" + postTitle + "' 거래가 완료되었습니다. 후기를 남겨보세요.",
                    "/market/chat/" + chatId
            );
            notificationService.create(
                    chat.getBuyer(), NotificationType.MESSAGE,
                    "거래가 완료되었어요",
                    "'" + postTitle + "' 거래가 완료되었습니다. 후기를 남겨보세요.",
                    "/market/chat/" + chatId
            );
        }
    }

    // ── 내 채팅 목록 ──
    @Transactional(readOnly = true)
    public Page<MarketChat> getMyChats(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        return marketChatRepository.findMyChats(currentUser.getId(), pageable);
    }

    // ── private 헬퍼 ──

    private boolean isParticipant(MarketChat chat, Long userId) {
        return chat.getSeller().getId().equals(userId)
                || chat.getBuyer().getId().equals(userId);
    }

    private User getOpponent(MarketChat chat, Long userId) {
        return chat.getSeller().getId().equals(userId)
                ? chat.getBuyer()
                : chat.getSeller();
    }

    @Transactional(readOnly = true)
    public int getChatCountByPost(Long postId) {
        return marketChatRepository.countByPost_Id(postId);
    }

    // ── 관리자: 채팅방 강제 종료 ──
    public void adminClose(Long chatId) {
        MarketChat chat = marketChatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("채팅방을 찾을 수 없습니다."));
        chat.close();
    }
}