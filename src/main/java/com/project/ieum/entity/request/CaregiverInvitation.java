package com.project.ieum.entity.request;

import com.project.ieum.entity.caregiver.CaregiverProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "caregiver_invitations",
    indexes = {
        @Index(name = "idx_inv_request_caregiver", columnList = "help_request_id,caregiver_id", unique = true)
    })
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaregiverInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "help_request_id", nullable = false)
    private HelpRequest helpRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_id", nullable = false)
    private CaregiverProfile caregiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void accept() { this.status = InvitationStatus.ACCEPTED; }
    public void reject() { this.status = InvitationStatus.REJECTED; }
}
