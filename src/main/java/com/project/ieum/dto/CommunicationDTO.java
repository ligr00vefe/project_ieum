package com.project.ieum.dto;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunicationDTO {

    private List<Long> communicationMethodIds;
}
