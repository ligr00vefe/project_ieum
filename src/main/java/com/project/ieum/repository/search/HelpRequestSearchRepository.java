package com.project.ieum.repository.search;

import com.project.ieum.dto.search.HelpRequestSearchCondition;
import com.project.ieum.entity.request.HelpRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HelpRequestSearchRepository {
    Page<HelpRequest> searchHelpRequests(HelpRequestSearchCondition condition, Pageable pageable);
}
