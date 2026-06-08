package com.project.ieum.repository;

import com.project.ieum.entity.request.HelpRequestPersonalityTag;
import com.project.ieum.entity.request.HelpRequestPersonalityTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpRequestPersonalityTagRepository extends JpaRepository<HelpRequestPersonalityTag, HelpRequestPersonalityTagId> {
    List<HelpRequestPersonalityTag> findByHelpRequest_Id(Long helpRequestId);
}
