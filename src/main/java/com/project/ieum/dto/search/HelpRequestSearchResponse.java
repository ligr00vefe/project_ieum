package com.project.ieum.dto.search;

import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HelpRequestSearchResponse {
    private Long helpRequestId;
    private String title;
    private String requesterName;
    private String regionName;
    private String serviceCategoryName;
    private LocalDateTime desiredStartDatetime;
    private LocalDateTime desiredEndDatetime;
    private HelpRequestStatus status;

    public static HelpRequestSearchResponse from(HelpRequest request) {
        String regionName = request.getSido() + " " + request.getSigungu()
                + (request.getBname() == null ? "" : " " + request.getBname());

        return HelpRequestSearchResponse.builder()
                .helpRequestId(request.getId())
                .title(request.getTitle())
                .requesterName(request.getRequester().getFullName())
                .regionName(regionName)
                .serviceCategoryName(request.getServiceCategory().getName())
                .desiredStartDatetime(request.getDesiredStartDatetime())
                .desiredEndDatetime(request.getDesiredEndDatetime())
                .status(request.getStatus())
                .build();
    }
}
