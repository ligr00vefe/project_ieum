package com.project.ieum.entity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // 💡 복합키 클래스는 동등성 비교를 위해 equals & hashCode 구현이 필수입니다.
public class UserCommunicationMethodId implements Serializable {

    @Column(name = "user_id") // ⭕ 명시적 컬럼 매핑으로 꼬임 방지
    private Long userId;

    @Column(name = "communication_method_id") // ⭕ 명시적 컬럼 매핑으로 꼬임 방지
    private Long communicationMethodId;
}