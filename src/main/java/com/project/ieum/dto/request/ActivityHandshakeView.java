package com.project.ieum.dto.request;

/**
 * 활동 시작/종료 양측 확인(핸드셰이크)의 템플릿 노출용 파생 뷰 상태.
 *
 * <p>raw 확인 플래그를 직접 노출하지 않고, 현재 사용자 관점에서 해석한 결과만 담는다.
 * 컨트롤러가 {@code MatchingService.getHandshakeView}로 채워 모델에 넣는다.
 *
 * @param visible        패널 표시 여부(MATCHED 또는 IN_PROGRESS이고 현재 사용자가 매칭 참여자일 때만 true)
 * @param phase          확인 단계: {@code "START"}(MATCHED) 또는 {@code "END"}(IN_PROGRESS)
 * @param myConfirmed    현재 사용자가 이미 확인했는가
 * @param otherConfirmed 상대가 이미 확인했는가
 * @param bothConfirmed  양측 모두 확인했는가(전이 직후 잠깐 보일 수 있음)
 */
public record ActivityHandshakeView(
        boolean visible,
        String phase,
        boolean myConfirmed,
        boolean otherConfirmed,
        boolean bothConfirmed) {

    public static ActivityHandshakeView hidden() {
        return new ActivityHandshakeView(false, null, false, false, false);
    }

    /** 현재 사용자가 지금 확인 버튼을 누를 수 있는가(표시 중 + 본인 미확인). */
    public boolean canConfirm() {
        return visible && !myConfirmed;
    }
}
