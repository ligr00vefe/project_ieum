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

  // 평점 (1~5)
  @Min(1) @Max(5)
  @Column(nullable = false, columnDefinition = "SMALLINT CHECK (rating BETWEEN 1 AND 5)")
  private Short rating;

  // 후기 본문
  @Column(columnDefinition = "TEXT")
  private String body;

  // 공개 여부
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ReviewVisibility visibility;
}
