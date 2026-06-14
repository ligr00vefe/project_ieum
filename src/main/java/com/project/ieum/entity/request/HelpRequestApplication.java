package com.project.ieum.entity.request;

import com.project.ieum.entity.ApplicationStatus;
import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "help_request_applications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_hra_request_caregiver", columnNames = {"help_request_id", "caregiver_id"})
    },
    indexes = {
        @Index(name = "idx_hra_caregiver_status", columnList = "caregiver_id,status,created_at")
    })
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class HelpRequestApplication extends BasicEntity {

  // 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 도움요청
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "help_request_id", nullable = false)
  @ToString.Exclude
  private HelpRequest helpRequest;

  // 활동지원사 (지원사 프로필)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "caregiver_id", nullable = false)
  @ToString.Exclude
  private CaregiverProfile caregiver;

  // 상태 (pending/accepted/rejected/withdrawn/completed/cancelled)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private ApplicationStatus status;

  public void accept()    { this.status = ApplicationStatus.ACCEPTED; }
  public void pending()   { this.status = ApplicationStatus.PENDING; }
  public void reject()    { this.status = ApplicationStatus.REJECTED; }
  public void complete()  { this.status = ApplicationStatus.COMPLETED; }
  // 취소: 요청자 마감(타 지원 선택)·요청 취소·지원자 본인 취소 모두 CANCELLED로 통합(구 withdraw 제거)
  public void cancel()    { this.status = ApplicationStatus.CANCELLED; }
}
