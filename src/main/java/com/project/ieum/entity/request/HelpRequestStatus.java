package com.project.ieum.entity.request;

public enum HelpRequestStatus {
  // 이용자가 게시하였을 때 기본값
  OPEN,
  // 이용자가 도움 지원 신청을 선택하여 마감됨
  MATCHED,
  // 이용자가 명시적 시작
  IN_PROGRESS,
  // 이용자가 명시적 종료 또는 희망 종료 시간으로부터 약 4시간 경과 시 자동 전환
  COMPLETED,
  // 사용자가 취소(삭제)함 또는 OPEN 상태인 채로 요청 시작 시간에 도달함.
  CLOSED
}
