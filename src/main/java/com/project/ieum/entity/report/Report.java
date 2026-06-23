package com.project.ieum.entity.report;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * 사용자 신고. 신고자(reporter)가 피신고자(target)를 사유와 함께 신고하고, 관리자가 상태를 전이한다.
 * "차단"은 별도 엔티티 없이 관리자 검토 후 기존 {@link com.project.ieum.entity.UserStatus#BANNED} 처리로 수행한다.
 */
@Entity
@Table(name = "reports",
        indexes = {
                @Index(name = "idx_report_status_created", columnList = "status,created_at"),
                @Index(name = "idx_report_target", columnList = "target_id")
        })
@Getter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Report extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    @ToString.Exclude
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    @ToString.Exclude
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReportReason reason;

    @Column(length = 1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private ReportStatus status = ReportStatus.RECEIVED;

    // 신고 맥락(대화방 id). 프로필에서의 신고 등 맥락이 없으면 null.
    @Column(name = "conversation_id")
    private Long conversationId;

    public void changeStatus(ReportStatus status) {
        this.status = status;
    }
}
