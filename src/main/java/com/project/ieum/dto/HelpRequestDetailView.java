package com.project.ieum.dto;

import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;

import java.time.LocalDateTime;

// 도움 요청 상세(게시자 본인 또는 도우미가 보는 상세 페이지).
// 리스트와 달리 전체 위치(roadAddress 등)를 노출한다.
// canClose/canDelete는 뷰어 권한·상태에 따라 팩토리에서 계산(엔티티에 보관하지 않음).
public record HelpRequestDetailView(
    Long id,
    String serviceCategoryName,
    String title,
    String body,
    String specialNotes,
    LocalDateTime desiredStartDatetime,
    LocalDateTime desiredEndDatetime,
    HelpRequestStatus status,
    // 전체 위치 (상세 한정 노출)
    String roadAddress,
    String addressDetail,
    // 게시자 표시명 = requester.fullName
    String ownerName,
    LocalDateTime createdAt,
    // 뷰어가 이 요청을 마감(CLOSE)할 수 있는가 = 게시자 && 활성 상태(OPEN)
    boolean canClose,
    // 뷰어가 하드 삭제할 수 있는가 = 게시자 && status == CLOSED (CLOSE된 뒤에만 하드 삭제)
    boolean canDelete
) {
    public static HelpRequestDetailView from(HelpRequest hr, Long viewerId) {
        boolean isOwner = viewerId != null && viewerId.equals(hr.getRequester().getUserId());
        boolean isClosable = isOwner && hr.getStatus() == HelpRequestStatus.OPEN;
        boolean isDeletable = isOwner && hr.getStatus() == HelpRequestStatus.CLOSED;
        return new HelpRequestDetailView(
            hr.getId(),
            hr.getServiceCategory().getName(),
            hr.getTitle(),
            hr.getBody(),
            hr.getSpecialNotes(),
            hr.getDesiredStartDatetime(),
            hr.getDesiredEndDatetime(),
            hr.getStatus(),
            hr.getRoadAddress(),
            hr.getAddressDetail(),
            hr.getRequester().getFullName(),
            hr.getCreatedAt(),
            isClosable,
            isDeletable
        );
    }
}
