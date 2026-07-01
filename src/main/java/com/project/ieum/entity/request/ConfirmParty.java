package com.project.ieum.entity.request;

/**
 * 활동 시작/종료 양측 확인(핸드셰이크)에서 확인 주체를 가리킨다.
 *
 * <p>{@code REQUESTER}=이용자(요청 작성자), {@code CAREGIVER}=활동지원사(선정된 활동지원사).
 * 서비스가 현재 로그인 사용자를 이 둘 중 하나로 해석해 확인 플래그를 세운다.
 */
public enum ConfirmParty {
    REQUESTER,
    CAREGIVER
}
