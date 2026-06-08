package com.project.ieum.entity;

public enum ApplicationStatus {
  // 신청중 / 선택됨 / 거부됨(요청자 명시적 거절·UX 주의) / 완료됨 / 취소됨(요청자 마감·요청취소·지원자 본인취소 통합)
  PENDING, ACCEPTED, REJECTED, COMPLETED, CANCELLED
}
