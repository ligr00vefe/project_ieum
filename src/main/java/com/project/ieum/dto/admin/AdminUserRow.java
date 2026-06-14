package com.project.ieum.dto.admin;

import com.project.ieum.entity.User;

import java.time.LocalDateTime;

public record AdminUserRow(
        Long id,
        String email,
        String roleLabel,
        String roleClass,
        String statusLabel,
        String statusClass,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static AdminUserRow from(User u) {
        String roleName = u.getRole() != null ? u.getRole().name() : "";
        String statusName = u.getStatus() != null ? u.getStatus().name() : "";

        String roleLabel = switch (roleName) {
            case "USER" -> "이용자";
            case "CAREGIVER" -> "활동지원사";
            default -> "관리자";
        };
        String roleClass = switch (roleName) {
            case "USER" -> "bg-indigo-50 text-indigo-600";
            case "CAREGIVER" -> "bg-amber-50 text-amber-600";
            default -> "bg-slate-100 text-slate-600";
        };
        String statusLabel = switch (statusName) {
            case "ACTIVE" -> "활성";
            case "PAUSED" -> "일시중지";
            case "BANNED" -> "정지";
            default -> "탈퇴";
        };
        String statusClass = switch (statusName) {
            case "ACTIVE" -> "bg-teal-50 text-teal-600";
            case "BANNED" -> "bg-red-50 text-red-500";
            default -> "bg-slate-100 text-slate-500";
        };

        return new AdminUserRow(u.getId(), u.getEmail(),
                roleLabel, roleClass, statusLabel, statusClass,
                u.getCreatedAt(), u.getLastLoginAt());
    }
}
