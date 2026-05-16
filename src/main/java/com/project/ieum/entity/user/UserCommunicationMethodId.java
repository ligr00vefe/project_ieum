package com.project.ieum.entity.user;

import lombok.*;

import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserCommunicationMethodId {
    private Long userId;
    private Long communicationMethodId;
}
