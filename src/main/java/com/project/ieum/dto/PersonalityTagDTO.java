package com.project.ieum.dto;

import lombok.*;

import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalityTagDTO {

    private List<Long> personalityTagIds;
}
