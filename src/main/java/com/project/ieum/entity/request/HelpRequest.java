package com.project.ieum.entity.request;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.Region;
import com.project.ieum.entity.user.UserProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "help_requests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_hr_requester_date", columnNames = {"requester_id", "desired_date"})
    },
    indexes = {
        @Index(name = "idx_hr_status_date", columnList = "status,desired_date")
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

  // 지역
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "region_id", nullable = false)
  @ToString.Exclude
  private Region region;

  // 제목
  @Column(nullable = false, length = 120)
  private String title;

  // 내용
  @Column(columnDefinition = "TEXT")
  private String body;

  // 희망 날짜 (요청자당 1건)
  @Column(name = "desired_date", nullable = false)
  private LocalDate desiredDate;

  // 시작 시각
  @Column(name = "desired_start_time")
  private LocalTime desiredStartTime;

  // 예상 종료 시각
  @Column(name = "desired_end_time")
  private LocalTime desiredEndTime;

  // 상세주소
  @Column(name = "address_detail", length = 255)
  private String addressDetail;

  // 특이사항
  @Column(name = "special_notes", columnDefinition = "TEXT")
  private String specialNotes;

  // 상태 (open/matched/in_progress/completed/cancelled/closed)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private HelpRequestStatus status;

  public void changeStatus(HelpRequestStatus next) { this.status = next; }
}
