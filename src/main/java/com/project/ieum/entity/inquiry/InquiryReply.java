package com.project.ieum.entity.inquiry;

import com.project.ieum.entity.BasicEntity;
import com.project.ieum.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiry_replies")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class InquiryReply extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private User answeredBy;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    public void updateBody(String body) { this.body = body; }
}
