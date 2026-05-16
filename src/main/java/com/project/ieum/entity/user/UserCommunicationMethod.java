package com.project.ieum.entity.user;

import com.project.ieum.entity.CommunicationMethod;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_communication_methods")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class UserCommunicationMethod {

    @EmbeddedId
    private UserCommunicationMethodId id;

    // 복합키(UserCommunicationMethodId) 내의 user_id 컬럼과 매핑되는 읽기전용 연관관계
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @ToString.Exclude
    private UserProfile user;

    // 복합키(UserCommunicationMethodId) 내의 communication_method_id 컬럼과 매핑되는 읽기전용 연관관계
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "communication_method_id", insertable = false, updatable = false)
    @ToString.Exclude
    private CommunicationMethod communicationMethod;
}