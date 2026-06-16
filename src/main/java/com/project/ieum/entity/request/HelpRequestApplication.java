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

  // ── 활동 양측 확인(핸드셰이크) 플래그 ──
  // 선정된(ACCEPTED) 지원에 한해 의미를 가진다. 이용자·도우미가 각자 시작/종료를 확인하고,
  // 양측이 모두 확인되면 서비스가 HelpRequest를 전이한다(MATCHED→IN_PROGRESS, IN_PROGRESS→COMPLETED).
  @Column(name = "requester_start_confirmed", nullable = false)
  @Builder.Default
  private boolean requesterStartConfirmed = false;

  @Column(name = "caregiver_start_confirmed", nullable = false)
  @Builder.Default
  private boolean caregiverStartConfirmed = false;

  @Column(name = "requester_end_confirmed", nullable = false)
  @Builder.Default
  private boolean requesterEndConfirmed = false;

  @Column(name = "caregiver_end_confirmed", nullable = false)
  @Builder.Default
  private boolean caregiverEndConfirmed = false;

  public void accept()    { this.status = ApplicationStatus.ACCEPTED; }
  public void pending()   { this.status = ApplicationStatus.PENDING; }
  public void complete()  { this.status = ApplicationStatus.COMPLETED; }
  // 취소: 요청자 마감(타 지원 선택)·요청 취소·지원자 본인 취소 모두 CANCELLED로 통합(구 withdraw 제거)
  public void cancel()    { this.status = ApplicationStatus.CANCELLED; }

  // ── 확인 플래그 캡슐화 (raw 플래그 대신 주체 기준 메서드로만 조작/조회) ──
  public void confirmStartBy(ConfirmParty party) {
    if (party == ConfirmParty.REQUESTER) this.requesterStartConfirmed = true;
    else                                 this.caregiverStartConfirmed = true;
  }

  public void confirmEndBy(ConfirmParty party) {
    if (party == ConfirmParty.REQUESTER) this.requesterEndConfirmed = true;
    else                                 this.caregiverEndConfirmed = true;
  }

  public boolean bothStartConfirmed() { return requesterStartConfirmed && caregiverStartConfirmed; }
  public boolean bothEndConfirmed()   { return requesterEndConfirmed && caregiverEndConfirmed; }

  // 파생 조회(서비스/템플릿 뷰 상태 구성용).
  public boolean startConfirmedBy(ConfirmParty party) {
    return party == ConfirmParty.REQUESTER ? requesterStartConfirmed : caregiverStartConfirmed;
  }

  public boolean endConfirmedBy(ConfirmParty party) {
    return party == ConfirmParty.REQUESTER ? requesterEndConfirmed : caregiverEndConfirmed;
  }
}
