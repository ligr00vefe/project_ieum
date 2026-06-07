package com.project.ieum.repository;

import com.project.ieum.entity.CommunicationMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunicationMethodRepository extends JpaRepository<CommunicationMethod, Long> {
    List<CommunicationMethod> findAllByOrderBySortOrderAsc();
}
