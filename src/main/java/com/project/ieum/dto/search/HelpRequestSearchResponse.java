package com.project.ieum.dto.search;

import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class HelpRequestSearchResponse {
    private Long helpRequestId;
    private String title;
    private String requesterName;
    private String regionName;
    private String serviceCategoryName;
    private LocalDate desiredDate;
    private LocalTime desiredStartTime;
    private LocalTime desiredEndTime;
    private HelpRequestStatus status;

    public static HelpRequestSearchResponse from(HelpRequest request) {
        String regionName = request.getRegion().getSido() + " " + request.getRegion().getSigungu()
                + (request.getRegion().getDong() == null ? "" : " " + request.getRegion().getDong());

        return HelpRequestSearchResponse.builder()
                .helpRequestId(request.getId())
                .title(request.getTitle())
                .requesterName(request.getRequester().getFullName())
                .regionName(regionName)
                .serviceCategoryName(request.getServiceCategory().getNameKo())
                .desiredDate(request.getDesiredDate())
                .desiredStartTime(request.getDesiredStartTime())
                .desiredEndTime(request.getDesiredEndTime())
                .status(request.getStatus())
                .build();
    }
}
