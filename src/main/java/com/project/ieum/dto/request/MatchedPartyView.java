package com.project.ieum.dto.request;

/**
 * 매칭 상세(이용자 측)에서 선정된 도우미와 대화방을 보여주기 위한 파생 뷰.
 *
 * <p>요청이 MATCHED 또는 IN_PROGRESS일 때만 의미가 있으며, 컨트롤러가
 * {@code MatchingService.getMatchedParty}로 채워 모델에 넣는다.
 *
 * @param caregiverName  선정된 도우미(활동지원사) 이름
 * @param conversationId 이용자-도우미 1:1 대화방 ID(없으면 null)
 */
public record MatchedPartyView(String caregiverName, Long conversationId) {
}
