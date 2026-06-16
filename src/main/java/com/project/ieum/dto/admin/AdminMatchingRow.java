package com.project.ieum.dto.admin;

import com.project.ieum.entity.request.HelpRequest;

import java.time.LocalDateTime;

public record AdminMatchingRow(
        Long id,
        String title,
        String requesterName,
        String sido,
        String sigungu,
        LocalDateTime desiredStartDatetime,
        String statusLabel,
        String statusClass
) {
    public static AdminMatchingRow from(HelpRequest r) {
        String requesterName = (r.getRequester() != null && r.getRequester().getFullName() != null)
                ? r.getRequester().getFullName() : "-";

        String statusName = r.getStatus() != null ? r.getStatus().name() : "";
        String statusLabel = switch (statusName) {
            case "OPEN" -> "모집중";
            case "MATCHED" -> "매칭완료";
            case "IN_PROGRESS" -> "진행중";
            case "COMPLETED" -> "완료";
            default -> "종료";
        };
        String statusClass = switch (statusName) {
            case "OPEN" -> "bg-indigo-50 text-indigo-600";
            case "MATCHED" -> "bg-teal-50 text-teal-600";
            case "IN_PROGRESS" -> "bg-teal-50 text-teal-600";
            case "COMPLETED" -> "bg-slate-100 text-slate-500";
            default -> "bg-slate-100 text-slate-400";
        };

        return new AdminMatchingRow(r.getId(), r.getTitle(), requesterName,
                r.getSido(), r.getSigungu(), r.getDesiredStartDatetime(),
                statusLabel, statusClass);
    }
}
