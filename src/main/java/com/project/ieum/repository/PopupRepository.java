package com.project.ieum.repository;

import com.project.ieum.entity.popup.Popup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PopupRepository extends JpaRepository<Popup, Long> {
    List<Popup> findByEnabledTrueAndExpiresAtAfter(java.time.LocalDateTime now);
    List<Popup> findByEnabledTrue();
    Page<Popup> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByEnabledTrueAndExpiresAtAfter(java.time.LocalDateTime now);
}
