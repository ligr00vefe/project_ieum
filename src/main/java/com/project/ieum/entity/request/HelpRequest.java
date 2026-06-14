package com.project.ieum.entity.request;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.user.UserProfile;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "help_requests",
    // (#9) requester+시작시각 unique 제거 → "시간대 겹침 거부"는 서비스 계층 검사로 대체
    //      (단순 동일시각 중복이 아니라 [start,end] 구간 겹침을 막아야 하므로 DB unique로는 부족)
    indexes = {
        @Index(name = "idx_hr_status_datetime", columnList = "status,desired_start_datetime")
    })
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class HelpRequest extends BasicEntity {

  // 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 요청자 (이용자 프로필)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "requester_id", nullable = false)
  @ToString.Exclude
  private UserProfile requester;

  // 서비스 분류
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "service_category_id", nullable = false)
  @ToString.Exclude
  private ServiceCategory serviceCategory;

  // 제목
  @Column(nullable = false, length = 120)
  private String title;

  // 내용
  @Column(columnDefinition = "TEXT")
  private String body;

  // 희망 시작 일시 (date+time 통합)
  @Column(name = "desired_start_datetime", nullable = false)
  private LocalDateTime desiredStartDatetime;

  // 예상 종료 일시
  @Column(name = "desired_end_datetime")
  private LocalDateTime desiredEndDatetime;

  // ── 위치 스냅샷 (write-once 비정규화 · region FK 대체, #10/D1=i·D2=b) ──
  // TODO(region 개편): 생성 시점에 채움 — 역할 분담:
  //   · 주소 구성요소(road_address·sido·sigungu·bname·zonecode·bcode) = 주소검색 위젯(Daum/Kakao 우편번호 서비스)
  //   · 좌표(latitude·longitude) = TMap 지오코딩(GeocodingService)  ← TMap은 좌표만, 우편/법정동코드는 미제공
  //   거리는 발견/정렬용(매칭 게이트 아님).

  // 도로명주소 (위치 식별 필수 — 주소검색 위젯이 채움)
  @Column(name = "road_address", nullable = false, length = 255)
  private String roadAddress;

  // 상세주소 (건물명+상세 · 사용자 입력 선택)
  @Column(name = "address_detail", length = 255)
  private String addressDetail;

  // 시/도 (표시·그룹 · 필수)
  @Column(name = "sido", nullable = false, length = 20)
  private String sido;

  // 시/군/구 (표시·그룹 · 필수)
  @Column(name = "sigungu", nullable = false, length = 40)
  private String sigungu;

  // 법정동
  @Column(name = "bname", length = 40)
  private String bname;

  // 우편번호
  @Column(name = "zonecode", length = 5)
  private String zonecode;

  // 법정동코드 (선택, 후속 통계/필터)
  @Column(name = "bcode", length = 10)
  private String bcode;

  // 위도 (근처 정렬용)
  @Column(name = "latitude", precision = 9, scale = 6)
  private BigDecimal latitude;

  // 경도
  @Column(name = "longitude", precision = 9, scale = 6)
  private BigDecimal longitude;

  // 출발지 도로명주소
  @Column(name = "departure_address", length = 255)
  private String departureAddress;

  // 도착지 도로명주소
  @Column(name = "destination_address", length = 255)
  private String destinationAddress;

  // 특이사항
  @Column(name = "special_notes", columnDefinition = "TEXT")
  private String specialNotes;

  // 상태 (open/matched/in_progress/completed/closed)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  @Builder.Default
  private HelpRequestStatus status = HelpRequestStatus.OPEN;

  public void changeStatus(HelpRequestStatus next) { this.status = next; }

  // 시간 구간 불변식: 종료가 있으면 시작 이후여야 한다.
  // existsOverlapping(반열림 겹침)이 "start <= end"를 전제하므로, end < start인 행은
  // 겹침을 조용히 누락시킨다. 이 콜백이 우회 불가 최종 방어선이다.
  @PrePersist
  @PreUpdate
  protected void verifyTimeRange() {
    if (desiredEndDatetime != null && desiredEndDatetime.isBefore(desiredStartDatetime)) {
      throw new IllegalStateException("종료 시간은 시작 시간 이후여야 합니다.");
    }
  }

  // (이슈 #8 정본) 본문 수정 메서드(updateDetails)는 제거함.
  // 근거: HelpRequest는 write-once — 도우미가 본 내용/시간/위치가 지원 후 바뀌면 신뢰성이 깨지므로,
  //       생성 후에는 상태 전이(changeStatus)만 허용한다. 수정이 필요하면 마감(close) 후 재작성.
}
