package com.project.ieum.repository;

import com.project.ieum.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findAllByOrderBySidoAscSigunguAscDongAsc();
}
