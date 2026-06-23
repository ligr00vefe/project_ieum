package com.project.ieum.dto.report;

import com.project.ieum.entity.report.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 사용자 신고 제출 폼. 대화방/프로필에서 전송한다.
 * conversationId는 대화방 맥락이 있을 때만 채워진다(프로필 신고 등은 null).
 */
public record ReportForm(
        @NotNull Long targetUserId,
        Long conversationId,
        @NotNull ReportReason reason,
        @Size(max = 1000, message = "상세 사유는 1000자 이내로 입력해 주세요.") String detail
) {}
