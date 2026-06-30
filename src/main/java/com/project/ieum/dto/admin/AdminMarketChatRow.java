package com.project.ieum.dto.admin;

import com.project.ieum.entity.market.MarketChat;

import java.time.LocalDateTime;

public record AdminMarketChatRow(
        Long id,
        String postTitle,
        boolean sharing,
        String postStatusLabel,
        String postStatusClass,
        String registrantEmail,
        String receiverEmail,
        String chatStatusLabel,
        String chatStatusClass,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {
    public static AdminMarketChatRow from(MarketChat c) {
        boolean sharing = c.getPost().isSharing();
        String postStatus = c.getPost().getStatus().name();
        String postLabel = switch (postStatus) {
            case "ACTIVE"   -> sharing ? "나눔중"   : "판매중";
            case "RESERVED" -> "예약중";
            case "SOLD"     -> sharing ? "나눔완료" : "판매완료";
            case "REMOVED"  -> "삭제됨";
            default         -> postStatus;
        };
        String postCls = switch (postStatus) {
            case "ACTIVE"   -> "bg-green-50 text-green-600";
            case "RESERVED" -> "bg-amber-50 text-amber-600";
            case "SOLD"     -> "bg-slate-100 text-slate-500";
            case "REMOVED"  -> "bg-red-50 text-red-500";
            default         -> "bg-slate-100 text-slate-400";
        };
        String chatStatus = c.getStatus().name();
        String chatLabel = "ACTIVE".equals(chatStatus) ? "진행중" : "종료";
        String chatCls   = "ACTIVE".equals(chatStatus) ? "bg-teal-50 text-teal-600" : "bg-slate-100 text-slate-400";

        return new AdminMarketChatRow(
                c.getId(),
                c.getPost().getTitle(),
                sharing,
                postLabel, postCls,
                c.getSeller() != null ? c.getSeller().getEmail() : "-",
                c.getBuyer()  != null ? c.getBuyer().getEmail()  : "-",
                chatLabel, chatCls,
                c.getLastMessageAt(),
                c.getCreatedAt()
        );
    }
}
