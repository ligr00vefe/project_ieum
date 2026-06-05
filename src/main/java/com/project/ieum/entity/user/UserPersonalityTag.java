package com.project.ieum.entity.user;

import com.project.ieum.entity.PersonalityTag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_personality_tags")
@IdClass(UserPersonalityTagId.class)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class UserPersonalityTag {

    // 이용자 프로필 (PK, FK)
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private UserProfile user;

    // 성향 태그 (PK, FK)
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id")
    @ToString.Exclude
    private PersonalityTag tag;
}
