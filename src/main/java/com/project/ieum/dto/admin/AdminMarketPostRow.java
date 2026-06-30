package com.project.ieum.dto.admin;

import com.project.ieum.entity.market.MarketPost;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminMarketPostRow(
        Long id,
        String title,
        String sellerEmail,
        String categoryName,
        BigDecimal price,
        boolean sharing,
        String statusLabel,
        String statusClass,
        LocalDateTime createdAt
) {
    public static AdminMarketPostRow from(MarketPost p) {
        boolean sharing = p.isSharing();
        String statusName = p.getStatus().name();
        String label = switch (statusName) {
            case "ACTIVE"   -> sharing ? "나눔중"   : "판매중";
            case "RESERVED" -> "예약중";
            case "SOLD"     -> sharing ? "나눔완료" : "판매완료";
            case "REMOVED"  -> "삭제됨";
            default         -> statusName;
        };
        String cls = switch (statusName) {
            case "ACTIVE"   -> "bg-green-50 text-green-600";
            case "RESERVED" -> "bg-amber-50 text-amber-600";
            case "SOLD"     -> "bg-slate-100 text-slate-500";
            case "REMOVED"  -> "bg-red-50 text-red-500";
            default         -> "bg-slate-100 text-slate-400";
        };
        return new AdminMarketPostRow(
                p.getId(),
                p.getTitle(),
                p.getSeller() != null ? p.getSeller().getEmail() : "-",
                p.getCategory() != null ? p.getCategory().getName() : "-",
                p.getPrice(),
                sharing,
                label, cls,
                p.getCreatedAt()
        );
    }
}
