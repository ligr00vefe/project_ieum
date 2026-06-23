// 위치: src/main/java/com/project/ieum/repository/market/MarketCategoryRepository.java
package com.project.ieum.repository.market;

import com.project.ieum.entity.market.MarketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketCategoryRepository extends JpaRepository<MarketCategory, Long> {
    // JpaRepository 기본 메서드(findAll, findById, save)만으로 충분
}