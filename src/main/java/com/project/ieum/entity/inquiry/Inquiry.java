package com.project.ieum.entity.inquiry;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiries",
    indexes = @Index(name = "idx_inquiry_status_created", columnList = "status,created_at"))
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Inquiry extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "is_secret", nullable = false)
    @Builder.Default
    private Boolean isSecret = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.PENDING;

    @OneToOne(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private InquiryReply reply;

    public void markAnswered() { this.status = InquiryStatus.ANSWERED; }
}
