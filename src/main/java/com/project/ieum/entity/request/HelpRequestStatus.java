package com.project.ieum.entity.request;

public enum HelpRequestStatus {
  // 이용자가 게시한 직후 기본값 (지원 모집 중)
  OPEN,
  // 이용자가 지원자를 선정하여 매칭됨 (나머지 지원은 자동 취소)
  MATCHED,
  // 양측 시작 확인으로 활동 진행 중
  IN_PROGRESS,
  // 양측 종료 확인, 또는 희망 종료 시간 +30분 경과 시 자동 전환
  COMPLETED,
  // 취소/마감: 이용자 취소, OPEN 만료(희망시작 -1시간), 매칭 후 합의취소·노쇼(시작 +30분)
  CLOSED
}
