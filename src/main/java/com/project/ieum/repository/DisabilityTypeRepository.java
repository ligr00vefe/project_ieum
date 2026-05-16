package com.project.ieum.repository;

import com.project.ieum.entity.user.DisabilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisabilityTypeRepository extends JpaRepository<DisabilityType, Long> {
    List<DisabilityType> findAllByOrderBySortOrderAsc();
}
