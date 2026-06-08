package com.project.ieum.dto;

import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;

import java.time.LocalDateTime;

// 도움 요청 리스트 카드(도우미가 목록에서 보는 항목).
// 프라이버시: roadAddress(상세주소)는 노출하지 않고 거친 2depth(sido+sigungu)만 라벨로 보여준다.
// 읽기 전용 → record (CLAUDE.md §7). serviceCategory는 호출 측에서 로딩되어 있어야 한다.
public record HelpRequestCardView(
    Long id,
    String title,
    String serviceCategoryName,
    LocalDateTime desiredStartDatetime,
    LocalDateTime desiredEndDatetime,
    HelpRequestStatus status,
    // 거친 위치 라벨 = sido + " " + sigungu (예: "부산시 동래구"). 팩토리에서 조합.
    String locationLabel
) {
    public static HelpRequestCardView from(HelpRequest hr) {
        return  new HelpRequestCardView(
            hr.getId(),
            hr.getTitle(),
            hr.getServiceCategory().getName(),
            hr.getDesiredStartDatetime(),
            hr.getDesiredEndDatetime(),
            hr.getStatus(),
            hr.getSido() + " " + hr.getSigungu()
        );
    }
}
