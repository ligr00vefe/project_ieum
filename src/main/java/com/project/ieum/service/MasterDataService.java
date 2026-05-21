package com.project.ieum.service;

import com.project.ieum.entity.CommunicationMethod;
import com.project.ieum.entity.PersonalityTag;
import com.project.ieum.entity.Region;
import com.project.ieum.entity.request.ServiceCategory;
import com.project.ieum.entity.user.DisabilityType;
import com.project.ieum.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterDataService {

    private final DisabilityTypeRepository disabilityTypeRepository;
    private final CommunicationMethodRepository communicationMethodRepository;
    private final PersonalityTagRepository personalityTagRepository;
    private final RegionRepository regionRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;

    public List<DisabilityType> getAllDisabilityTypes() {
        return disabilityTypeRepository.findAllByOrderBySortOrderAsc();
    }

    public List<CommunicationMethod> getAllCommunicationMethods() {
        return communicationMethodRepository.findAllByOrderBySortOrderAsc();
    }

    public List<PersonalityTag> getAllPersonalityTags() {
        return personalityTagRepository.findAllByOrderByIdAsc();
    }

    public List<Region> getAllRegions() {
        return regionRepository.findAllByOrderBySidoAscSigunguAscDongAsc();
    }

    public List<ServiceCategory> getAllServiceCategories() {
        return serviceCategoryRepository.findAllByOrderByIdAsc();
    }
}
