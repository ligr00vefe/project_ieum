package com.project.ieum.entity.user;

import com.project.ieum.entity.CommunicationMethod;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_communication_methods")
@IdClass(UserCommunicationMethodId.class)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class UserCommunicationMethod {

    // 이용자 프로필 (PK, FK)
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private UserProfile user;

    // 의사소통 방식 (PK, FK)
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "communication_method_id")
    @ToString.Exclude
    private CommunicationMethod communicationMethod;
}
