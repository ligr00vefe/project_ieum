package com.project.ieum.controller.chat;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.conversation.Conversation;
import com.project.ieum.repository.ConversationRepository;
import com.project.ieum.service.MatchingService;
import com.project.ieum.service.common.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatPageController {

    private final ConversationRepository conversationRepository;
    private final CurrentUserService currentUserService;
    private final MatchingService matchingService;

    @GetMapping("/conversations")
    public String conversations(Model model) {
        model.addAttribute("title", "채팅");
        model.addAttribute("content", "chat/conversations");
        return "layout/layout";
    }

    @GetMapping("/conversations/{conversationId}")
    public String room(@PathVariable Long conversationId, Model model) {
        User currentUser = currentUserService.getCurrentUser();
        Conversation conversation = conversationRepository.findWithParticipantsById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));

        boolean isUser = currentUser.getRole() == UserRole.USER;
        boolean isCaregiver = currentUser.getRole() == UserRole.CAREGIVER;

        String partnerName;
        String partnerProfileImage;
        String partnerSubtitle;

        Long requestId = null;
        int matchPercent = 0;
        if (isUser) {
            var caregiver = conversation.getCaregiver();
            partnerName = caregiver.getFullName() + " 활동지원사";
            partnerProfileImage = caregiver.getProfileImageUrl();
            partnerSubtitle = (caregiver.getCertificationType() != null ? caregiver.getCertificationType() : "활동지원사")
                    + (caregiver.getExperience() != null ? " · 경력 " + caregiver.getExperience() : "");
            requestId = conversation.getApplication().getHelpRequest().getId();
        } else {
            var requester = conversation.getRequester();
            partnerName = requester.getFullName() + " 이용자";
            partnerProfileImage = requester.getProfileImageUrl();
            partnerSubtitle = "이용자";
            requestId = conversation.getApplication().getHelpRequest().getId();
        }

        if (requestId != null) {
            var applications = java.util.List.of(conversation.getApplication());
            var pctMap = matchingService.getMatchPercentMap(requestId, applications);
            matchPercent = pctMap.getOrDefault(conversation.getApplication().getId(), 0);
        }

        model.addAttribute("title", "채팅");
        model.addAttribute("conversationId", conversationId);
        model.addAttribute("isUser", isUser);
        model.addAttribute("isCaregiver", isCaregiver);
        model.addAttribute("partnerName", partnerName);
        model.addAttribute("partnerProfileImage", partnerProfileImage);
        model.addAttribute("partnerSubtitle", partnerSubtitle);
        model.addAttribute("matchPercent", matchPercent);
        model.addAttribute("requestId", requestId);
        model.addAttribute("content", "chat/room");
        return "layout/layout";
    }
}
