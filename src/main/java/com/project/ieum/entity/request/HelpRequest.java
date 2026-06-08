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

  // 특이사항
  @Column(name = "special_notes", columnDefinition = "TEXT")
  private String specialNotes;

  // 상태 (open/matched/in_progress/completed/cancelled/closed)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private HelpRequestStatus status;

  public void changeStatus(HelpRequestStatus next) { this.status = next; }

  public void updateDetails(
      ServiceCategory serviceCategory,
      Region region,
      String title,
      String body,
      LocalDate desiredDate,
      LocalTime desiredStartTime,
      LocalTime desiredEndTime,
      String addressDetail,
      String specialNotes) {
    this.serviceCategory = serviceCategory;
    this.region = region;
    this.title = title;
    this.body = body;
    this.desiredDate = desiredDate;
    this.desiredStartTime = desiredStartTime;
    this.desiredEndTime = desiredEndTime;
    this.addressDetail = addressDetail;
    this.specialNotes = specialNotes;
  }
}
