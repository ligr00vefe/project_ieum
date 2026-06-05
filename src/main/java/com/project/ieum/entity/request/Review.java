package com.project.ieum.entity.request;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.user.UserProfile;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "reviews",
    uniqueConstraints = @UniqueConstraint(name = "uq_review_help_request", columnNames = "help_request_id"),
    indexes = @Index(name = "idx_review_target", columnList = "target_id,created_at"))
@Check(name = "ck_review_rating", constraints = "rating BETWEEN 1 AND 5")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Review extends BasicEntity {

  // 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 도움요청 (요청당 1건)
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "help_request_id", nullable = false)
  @ToString.Exclude
  private HelpRequest helpRequest;

  // 작성자 (요청자 = 이용자 프로필)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  @ToString.Exclude
  private UserProfile author;

  // 대상자 (채택된 활동지원사 프로필)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "target_id", nullable = false)
  @ToString.Exclude
  private CaregiverProfile target;

  // 평점 (1~5) — DB 제약은 클래스 @Check, 애플리케이션 검증은 @Min/@Max
  @Min(1) @Max(5)
  @Column(nullable = false)
  private Short rating;

  // 후기 본문
  @Column(columnDefinition = "TEXT")
  private String body;

  // 공개 여부
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ReviewVisibility visibility;

  // 작성자 수정(오타·평점 정정). 인가/수정가능기간/평점 재집계는 서비스 책임(Q2 후속).
  public void edit(Short rating, String body) {
    this.rating = rating;
    this.body = body;
  }

  // 공개/숨김 전환 — 하드삭제 대신 PRIVATE 소프트숨김에 사용(집계 무결성 보존).
  public void changeVisibility(ReviewVisibility visibility) {
    this.visibility = visibility;
  }
}
