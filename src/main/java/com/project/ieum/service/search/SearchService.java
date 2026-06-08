package com.project.ieum.service.search;

import com.project.ieum.dto.search.CaregiverSearchCondition;
import com.project.ieum.dto.search.CaregiverSearchResponse;
import com.project.ieum.dto.search.HelpRequestSearchCondition;
import com.project.ieum.dto.search.HelpRequestSearchResponse;
import com.project.ieum.repository.CaregiverProfileRepository;
import com.project.ieum.repository.HelpRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final CaregiverProfileRepository caregiverProfileRepository;
    private final HelpRequestRepository helpRequestRepository;

    public Page<CaregiverSearchResponse> searchCaregivers(CaregiverSearchCondition condition, Pageable pageable) {
        return caregiverProfileRepository.searchCaregivers(condition, pageable)
                .map(CaregiverSearchResponse::from);
    }

    public Page<HelpRequestSearchResponse> searchHelpRequests(HelpRequestSearchCondition condition, Pageable pageable) {
        return helpRequestRepository.searchHelpRequests(condition, pageable)
                .map(HelpRequestSearchResponse::from);
    }
}
